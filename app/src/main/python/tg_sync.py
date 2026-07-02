import asyncio
import os
from telethon import TelegramClient
from telethon.tl.types import MessageMediaDocument, DocumentAttributeAudio

# Generic testing API ID and Hash (usually fine for private personal tools)
api_id = 94575
api_hash = 'a3406de8d171bb422bb6ddf3bbd800e2'

client = None
loop = None
phone_hash_cache = None

def init_client(session_dir):
    global client, loop
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
        loop.run_until_complete(client.sign_in(phone, code, phone_code_hash=phone_hash_cache, password=password))
        return "SUCCESS"
    except Exception as e:
        return f"Error: {str(e)}"

def sync_chat(chat_id, target_dir, callback):
    if not client:
        callback.onProgress("Error: Client not initialized")
        return

    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    async def _sync():
        try:
            callback.onProgress("Connecting to Telegram...")
            if not await client.is_user_authorized():
                callback.onProgress("Error: Not logged in")
                return

            callback.onProgress(f"Fetching audio files from {chat_id}...")
            audio_files = []
            
            # Fetch all messages with media
            async for message in client.iter_messages(chat_id, filter=None):
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

            # 2. Download missing files from Telegram
            downloaded_count = 0
            for audio in audio_files:
                filename = audio["filename"]
                if filename not in local_files:
                    callback.onProgress(f"Downloading: {filename}...")
                    await client.download_media(audio["msg"], file=os.path.join(target_dir, filename))
                    downloaded_count += 1

            callback.onProgress(f"Sync Complete! Downloaded: {downloaded_count}, Deleted: {deleted_count}")

        except Exception as e:
            callback.onProgress(f"Error during sync: {str(e)}")

    loop.run_until_complete(_sync())
