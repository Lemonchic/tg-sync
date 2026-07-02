import asyncio
import os
from telethon import TelegramClient
from telethon.tl.types import MessageMediaDocument, DocumentAttributeAudio
from telethon.errors import SessionPasswordNeededError

# Generic testing API ID and Hash (usually fine for private personal tools)
api_id = 94575
api_hash = 'a3406de8d171bb422bb6ddf3bbd800e2'

client = None
loop = None
phone_hash_cache = None

def init_client(session_dir):
    global client, loop

    # Monkey-patch Telethon's slow pure-Python AES-IGE with our fast
    # cryptography-backed implementation BEFORE creating the client
    try:
        from fast_ige import patch_telethon
        result = patch_telethon()
        print(f"[KarooTgSync] {result}")
    except Exception as e:
        print(f"[KarooTgSync] Warning: fast_ige patch failed: {e}")

    if not os.path.exists(session_dir):
        os.makedirs(session_dir)
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    client = TelegramClient(f"{session_dir}/karoo_sync.session", api_id, api_hash, loop=loop)
    loop.run_until_complete(client.connect())

def request_code(phone):
    global phone_hash_cache
    if not client:
        return "Error: Client not initialized"
    try:
        req = loop.run_until_complete(client.send_code_request(phone))
        phone_hash_cache = req.phone_code_hash
        return "SUCCESS"
    except Exception as e:
        return f"Error: {str(e)}"

def submit_code(phone, code, password=""):
    if not client:
        return "Error: Client not initialized"
    try:
        if password:
            loop.run_until_complete(client.sign_in(password=password))
        else:
            loop.run_until_complete(client.sign_in(phone, code, phone_code_hash=phone_hash_cache))
        return "SUCCESS"
    except SessionPasswordNeededError:
        return "PASSWORD_NEEDED"
    except Exception as e:
        return f"Error: {str(e)}"

import re
import time

async def _sync_single_chat(chat_entity, target_dir, callback):
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    audio_files = []
    
    # Fetch all messages with media
    async for message in client.iter_messages(chat_entity, filter=None):
        if message.media and isinstance(message.media, MessageMediaDocument):
            for attr in message.media.document.attributes:
                if isinstance(attr, DocumentAttributeAudio):
                    filename = next((a.file_name for a in message.media.document.attributes if hasattr(a, 'file_name')), None)
                    if not filename:
                        filename = f"audio_{message.id}.mp3"
                    audio_files.append({"id": message.id, "filename": filename, "msg": message})
                    break

    callback.onProgress(f"Found {len(audio_files)} audio files on Telegram.")
    
    local_files = []
    if os.path.exists(target_dir):
        local_files = os.listdir(target_dir)

    remote_filenames = [a["filename"] for a in audio_files]

    # 1. Delete local files that no longer exist on Telegram
    deleted_count = 0
    for lf in local_files:
        if lf not in remote_filenames:
            callback.onProgress(f"Deleting missing file: {lf}")
            try:
                os.remove(os.path.join(target_dir, lf))
                deleted_count += 1
            except Exception as e:
                callback.onProgress(f"Error deleting {lf}: {e}")

    # 2. Download missing files from Telegram with speed tracking
    downloaded_count = 0
    for audio in audio_files:
        filename = audio["filename"]
        if filename not in local_files:
            dl_start = time.time()
            last_update = [dl_start]

            def make_progress_cb(fname, start_t, last_t):
                def progress_cb(received, total):
                    now = time.time()
                    if now - last_t[0] >= 1.0:
                        elapsed = now - start_t
                        speed = received / elapsed if elapsed > 0 else 0
                        total_mb = total / (1024 * 1024) if total else 0
                        recv_mb = received / (1024 * 1024)
                        if speed >= 1_000_000:
                            speed_str = f"{speed / 1_000_000:.2f} MB/s"
                        else:
                            speed_str = f"{speed / 1024:.0f} KB/s"
                        pct = (received / total * 100) if total else 0
                        callback.onProgress(
                            f"  ↓ {fname[:30]}  {recv_mb:.1f}/{total_mb:.1f}MB  {pct:.0f}%  @ {speed_str}"
                        )
                        last_t[0] = now
                return progress_cb

            callback.onProgress(f"Downloading: {filename}")
            await client.download_media(
                audio["msg"],
                file=os.path.join(target_dir, filename),
                progress_callback=make_progress_cb(filename, dl_start, last_update)
            )
            elapsed = time.time() - dl_start
            avg_speed = os.path.getsize(os.path.join(target_dir, filename)) / elapsed if elapsed > 0 else 0
            if avg_speed >= 1_000_000:
                avg_str = f"{avg_speed / 1_000_000:.2f} MB/s"
            else:
                avg_str = f"{avg_speed / 1024:.0f} KB/s"
            callback.onProgress(f"  ✓ Done in {elapsed:.1f}s (avg {avg_str})")
            downloaded_count += 1

    callback.onProgress(f"Sync Complete! Downloaded: {downloaded_count}, Deleted: {deleted_count}")

def sync_chat(chat_id, target_dir, callback):
    if not client:
        callback.onProgress("Error: Client not initialized")
        return

    async def _sync():
        try:
            callback.onProgress("Connecting to Telegram...")
            if not await client.is_user_authorized():
                callback.onProgress("Error: Not logged in")
                return

            if not chat_id:
                callback.onProgress("Finding Telegram folder 'Music'...")
                from telethon.tl.functions.messages import GetDialogFiltersRequest
                filters = await client(GetDialogFiltersRequest())
                
                music_filter = None
                for f in filters.filters:
                    if hasattr(f, 'title') and f.title:
                        title_text = f.title.text if hasattr(f.title, 'text') else str(f.title)
                        if title_text.lower() == "music":
                            music_filter = f
                            break
                
                if not music_filter:
                    callback.onProgress("Error: 'Music' folder not found in Telegram.")
                    return
                
                peer_ids = []
                for peer in music_filter.include_peers:
                    if hasattr(peer, 'user_id'):
                        peer_ids.append(peer.user_id)
                    elif hasattr(peer, 'chat_id'):
                        peer_ids.append(peer.chat_id)
                    elif hasattr(peer, 'channel_id'):
                        peer_ids.append(peer.channel_id)
                
                callback.onProgress(f"Found 'Music' folder with {len(peer_ids)} chats. Fetching dialogs...")
                dialogs = await client.get_dialogs()
                music_chats = [d for d in dialogs if d.entity.id in peer_ids]
                
                callback.onProgress(f"Syncing {len(music_chats)} chats in 'Music' folder...")
                for dialog in music_chats:
                    chat_title = dialog.name
                    clean_title = re.sub(r'[\\/*?:"<>|]', "", chat_title)
                    chat_target_dir = os.path.join(target_dir, clean_title)
                    callback.onProgress(f"\n--- Syncing: {chat_title} ---")
                    await _sync_single_chat(dialog.entity, chat_target_dir, callback)
            else:
                callback.onProgress(f"Syncing single chat: {chat_id}")
                await _sync_single_chat(chat_id, target_dir, callback)

        except Exception as e:
            callback.onProgress(f"Error during sync: {str(e)}")

    loop.run_until_complete(_sync())

def is_authorized():
    if not client:
        return False
    try:
        return loop.run_until_complete(client.is_user_authorized())
    except Exception:
        return False
