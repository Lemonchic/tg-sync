"""
Fast AES-IGE using the `cryptography` library's C-compiled AES-ECB backend.

Monkey-patches Telethon's AES class static methods to replace the pure-Python
pyaes fallback with fast native C encryption. This is the #1 bottleneck for
Telethon download speed on resource-constrained devices like the Karoo.
"""

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend


def fast_decrypt_ige(cipher_text, key, iv):
    """AES-256-IGE decrypt using cryptography's C-compiled AES-ECB."""
    iv1 = iv[:16]   # previous ciphertext block feedback
    iv2 = iv[16:]   # previous plaintext block feedback

    cipher = Cipher(algorithms.AES(key), modes.ECB(), backend=default_backend())
    decryptor = cipher.decryptor()

    plaintext = bytearray(len(cipher_text))

    for i in range(0, len(cipher_text), 16):
        block = cipher_text[i:i + 16]

        # XOR block with iv2, decrypt, XOR result with iv1
        xored = (int.from_bytes(block, 'big') ^ int.from_bytes(iv2, 'big')).to_bytes(16, 'big')
        decrypted = decryptor.update(xored)
        out = (int.from_bytes(decrypted, 'big') ^ int.from_bytes(iv1, 'big')).to_bytes(16, 'big')

        iv1 = block     # ciphertext feeds back
        iv2 = out       # plaintext feeds back

        plaintext[i:i + 16] = out

    decryptor.finalize()
    return bytes(plaintext)


def fast_encrypt_ige(plain_text, key, iv):
    """AES-256-IGE encrypt using cryptography's C-compiled AES-ECB."""
    import os as _os

    # Pad to 16-byte boundary
    padding = len(plain_text) % 16
    if padding:
        plain_text += _os.urandom(16 - padding)

    iv1 = iv[:16]   # previous ciphertext feedback
    iv2 = iv[16:]   # previous plaintext feedback

    cipher = Cipher(algorithms.AES(key), modes.ECB(), backend=default_backend())
    encryptor = cipher.encryptor()

    ciphertext = bytearray(len(plain_text))

    for i in range(0, len(plain_text), 16):
        block = plain_text[i:i + 16]

        # XOR block with iv1, encrypt, XOR result with iv2
        xored = (int.from_bytes(block, 'big') ^ int.from_bytes(iv1, 'big')).to_bytes(16, 'big')
        encrypted = encryptor.update(xored)
        out = (int.from_bytes(encrypted, 'big') ^ int.from_bytes(iv2, 'big')).to_bytes(16, 'big')

        iv1 = out       # ciphertext feeds back
        iv2 = block     # plaintext feeds back

        ciphertext[i:i + 16] = out

    encryptor.finalize()
    return bytes(ciphertext)


def patch_telethon():
    """
    Monkey-patch Telethon's AES class with our fast implementations.

    CRITICAL: Must patch AES.decrypt_ige and AES.encrypt_ige as static methods
    on the class, NOT as module-level functions. Telethon calls AES.decrypt_ige().
    """
    try:
        from telethon.crypto import aes as telethon_aes

        # Patch the CLASS static methods - this is what Telethon actually calls
        telethon_aes.AES.decrypt_ige = staticmethod(fast_decrypt_ige)
        telethon_aes.AES.encrypt_ige = staticmethod(fast_encrypt_ige)

        return "Patched Telethon AES.decrypt_ige/encrypt_ige with fast cryptography backend"
    except Exception as e:
        return f"Warning: Could not patch Telethon AES: {e}"
