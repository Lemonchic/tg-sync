import asyncio
import os
from telethon import TelegramClient
from telethon.tl.types import MessageMediaDocument, DocumentAttributeAudio
from telethon.errors import SessionPasswordNeededError

# Official Telegram Desktop API ID and Hash (used to bypass unofficial client code delivery blocks)
api_id = 2040
api_hash = 'b18441a1ff607e10a989891a5462e627'

import threading

client = None
loop = None
phone_hash_cache = None

def init_client(session_dir):
    global client, loop
    if client is not None:
        print("[KarooTgSync] Client already initialized, skipping duplicate setup")
        return

    # Disabled monkey-patch to prevent login packet corruption
    print("[KarooTgSync] AES-IGE monkey-patch disabled for safety")

    if not os.path.exists(session_dir):
        os.makedirs(session_dir)

    # 1. Create a dedicated event loop
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    # 2. Create the Telegram client bound to the loop
    client = TelegramClient(f"{session_dir}/karoo_sync.session", api_id, api_hash, loop=loop)
    
    # 3. Connect to Telegram
    loop.run_until_complete(client.connect())

def request_code(phone):
    global phone_hash_cache
    if not client:
        return "Error: Client not initialized"
    try:
        req = loop.run_until_complete(client.send_code_request(phone))
        phone_hash_cache = req.phone_code_hash
        # Extract the type name of SentCodeType (e.g., SentCodeTypeApp, SentCodeTypeSms)
        delivery_type = "App"
        if hasattr(req, 'type') and req.type:
            type_name = type(req.type).__name__
            if "Sms" in type_name:
                delivery_type = "SMS"
            elif "App" in type_name:
                delivery_type = "Telegram App"
            elif "Call" in type_name:
                delivery_type = "Phone Call"
            else:
                delivery_type = type_name
        print(f"[KarooTgSync] Code successfully sent via: {delivery_type}")
        return f"SUCCESS:{delivery_type}"
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
                    audio_files.append({"id": message.id, "filename": filename, "size": message.media.document.size, "msg": message})
                    break

    callback.onProgress(f"Found {len(audio_files)} audio files on Telegram.")
    
    local_files = []
    if os.path.exists(target_dir):
        local_files = os.listdir(target_dir)

    remote_filenames = [a["filename"] for a in audio_files]

    callback.onProgress(f"Local files ({len(local_files)}): {local_files[:5]}{'...' if len(local_files) > 5 else ''}")
    callback.onProgress(f"Remote files ({len(remote_filenames)}): {remote_filenames[:5]}{'...' if len(remote_filenames) > 5 else ''}")

    # 1. Delete local files that no longer exist on Telegram
    deleted_count = 0
    for lf in local_files:
        if lf.startswith('.'):
            continue
        if lf not in remote_filenames:
            filepath = os.path.join(target_dir, lf)
            if os.path.isfile(filepath):
                callback.onProgress(f"Deleting orphaned file: {lf}")
                try:
                    os.remove(filepath)
                    deleted_count += 1
                except Exception as e:
                    callback.onProgress(f"Error deleting {lf}: {e}")
    if deleted_count > 0:
        callback.onProgress(f"Deleted {deleted_count} orphaned files.")

    # 2. Download missing or invalid files from Telegram with speed tracking
    downloaded_count = 0
    total_tracks = len(audio_files)
    for idx, audio in enumerate(audio_files):
        filename = audio["filename"]
        size = audio["size"]
        local_path = os.path.join(target_dir, filename)

        needs_download = False
        if filename not in local_files:
            needs_download = True
        else:
            try:
                local_size = os.path.getsize(local_path)
                if local_size != size:
                    callback.onProgress(f"[{idx+1}/{total_tracks}] Size mismatch for '{filename}': local {local_size} bytes, remote {size} bytes. Re-downloading...")
                    needs_download = True
            except Exception as e:
                needs_download = True

        if needs_download:
            dl_start = time.time()
            last_update = [dl_start]

            def make_progress_cb(fname, start_t, last_t, track_num, total_num):
                last_bytes = [0]
                def progress_cb(received, total, active_conns=0):
                    now = time.time()
                    dt = now - last_t[0]
                    if dt >= 1.0:
                        delta_bytes = received - last_bytes[0]
                        speed = delta_bytes / dt if dt > 0 else 0
                        total_mb = total / (1024 * 1024) if total else 0
                        recv_mb = received / (1024 * 1024)
                        if speed >= 1_000_000:
                            speed_str = f"{speed / 1_000_000:.2f} MB/s"
                        else:
                            speed_str = f"{speed / 1024:.0f} KB/s"
                        pct = (received / total * 100) if total else 0
                        callback.onProgress(
                            f"  ↓ [{track_num}/{total_num}] {fname[:20]}  {recv_mb:.1f}/{total_mb:.1f}MB  {pct:.0f}%  @ {speed_str} [Conns: {active_conns}]"
                        )
                        last_t[0] = now
                        last_bytes[0] = received
                return progress_cb

            from fast_telethon import download_file

            callback.onProgress(f"[{idx+1}/{total_tracks}] Downloading: {filename}")
            filepath = os.path.join(target_dir, filename)
            try:
                with open(filepath, 'wb') as out_f:
                    await download_file(
                        client,
                        audio["msg"].media.document,
                        out_f,
                        progress_callback=make_progress_cb(filename, dl_start, last_update, idx+1, total_tracks)
                    )
            except Exception as e:
                callback.onProgress(f"  ✗ Error downloading {filename}: {e}")
                if os.path.exists(filepath):
                    try:
                        os.remove(filepath)
                    except:
                        pass
                continue

            elapsed = time.time() - dl_start
            avg_speed = os.path.getsize(os.path.join(target_dir, filename)) / elapsed if elapsed > 0 else 0
            if avg_speed >= 1_000_000:
                avg_str = f"{avg_speed / 1_000_000:.2f} MB/s"
            else:
                avg_str = f"{avg_speed / 1024:.0f} KB/s"
            callback.onProgress(f"  ✓ Done [{idx+1}/{total_tracks}] in {elapsed:.1f}s (avg {avg_str})")
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

                # 1. Map existing local folders by their ID
                local_folders_by_id = {}
                if os.path.exists(target_dir):
                    for entry in os.listdir(target_dir):
                        entry_path = os.path.join(target_dir, entry)
                        if os.path.isdir(entry_path):
                            id_file = os.path.join(entry_path, ".tg_channel_id")
                            if os.path.exists(id_file):
                                try:
                                    with open(id_file, 'r', encoding='utf-8') as f:
                                        cid = f.read().strip()
                                        if cid:
                                            local_folders_by_id[cid] = entry
                                except Exception:
                                    pass

                # 2. Iterate and check for rename or create
                active_ids = set()
                import shutil
                for dialog in music_chats:
                    cid = str(dialog.entity.id)
                    active_ids.add(cid)
                    
                    chat_title = dialog.name
                    clean_title = re.sub(r'[\\/*?:"<>|]', "", chat_title)
                    
                    chat_target_dir = None
                    if cid in local_folders_by_id:
                        existing_folder = local_folders_by_id[cid]
                        if existing_folder != clean_title:
                            # User renamed channel, rename folder on device
                            old_path = os.path.join(target_dir, existing_folder)
                            new_path = os.path.join(target_dir, clean_title)
                            callback.onProgress(f"Renaming folder '{existing_folder}' to '{clean_title}'...")
                            try:
                                if os.path.exists(new_path):
                                    shutil.copytree(old_path, new_path, dirs_exist_ok=True)
                                    shutil.rmtree(old_path)
                                else:
                                    os.rename(old_path, new_path)
                                callback.onProgress("✓ Renamed successfully.")
                            except Exception as e:
                                callback.onProgress(f"✗ Failed to rename folder: {e}")
                                clean_title = existing_folder
                        
                        chat_target_dir = os.path.join(target_dir, clean_title)
                    else:
                        # Chat ID not found in mapping. Check if folder name exists
                        chat_target_dir = os.path.join(target_dir, clean_title)
                        if os.path.exists(chat_target_dir):
                            callback.onProgress(f"Associating folder '{clean_title}' with chat ID {cid}")
                        else:
                            callback.onProgress(f"Creating new folder '{clean_title}' for chat ID {cid}")
                            os.makedirs(chat_target_dir, exist_ok=True)
                        
                        # Write the ID file
                        try:
                            id_file = os.path.join(chat_target_dir, ".tg_channel_id")
                            with open(id_file, 'w', encoding='utf-8') as f:
                                f.write(cid)
                        except Exception as e:
                            callback.onProgress(f"Warning: Failed to write ID file: {e}")

                    callback.onProgress(f"\n--- Syncing: {chat_title} ---")
                    await _sync_single_chat(dialog.entity, chat_target_dir, callback)

                # 3. Clean up orphaned folders
                if os.path.exists(target_dir):
                    removed_folders = 0
                    for entry in os.listdir(target_dir):
                        entry_path = os.path.join(target_dir, entry)
                        if os.path.isdir(entry_path):
                            id_file = os.path.join(entry_path, ".tg_channel_id")
                            if os.path.exists(id_file):
                                try:
                                    with open(id_file, 'r', encoding='utf-8') as f:
                                        cid = f.read().strip()
                                    if cid and cid not in active_ids:
                                        callback.onProgress(f"Removing orphaned folder: {entry}/")
                                        shutil.rmtree(entry_path)
                                        removed_folders += 1
                                except Exception as e:
                                    callback.onProgress(f"Error removing folder {entry}: {e}")
                    if removed_folders > 0:
                        callback.onProgress(f"Removed {removed_folders} orphaned folders.")

            else:
                callback.onProgress(f"Syncing single chat: {chat_id}")
                try:
                    entity = await client.get_input_entity(chat_id)
                    full_entity = await client.get_entity(entity)
                    cid = str(full_entity.id)
                    
                    if not os.path.exists(target_dir):
                        os.makedirs(target_dir, exist_ok=True)
                    id_file = os.path.join(target_dir, ".tg_channel_id")
                    with open(id_file, 'w', encoding='utf-8') as f:
                        f.write(cid)
                    await _sync_single_chat(entity, target_dir, callback)
                except Exception as e:
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

def logout():
    global client
    if not client:
        return "SUCCESS"
    try:
        loop.run_until_complete(client.log_out())
        return "SUCCESS"
    except Exception as e:
        return f"Error: {str(e)}"

def reset_client(session_dir):
    global client, loop
    if client is not None:
        try:
            loop.run_until_complete(client.disconnect())
        except Exception:
            pass
        client = None
    
    # Force delete session database files to ensure absolute clean state
    session_file = f"{session_dir}/karoo_sync.session"
    for ext in ['', '-journal']:
        path = session_file + ext
        if os.path.exists(path):
            try:
                os.remove(path)
            except Exception:
                pass

    init_client(session_dir)
    return "SUCCESS"
