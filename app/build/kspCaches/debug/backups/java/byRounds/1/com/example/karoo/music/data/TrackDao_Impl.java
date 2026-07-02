package com.example.karoo.music.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrackDao_Impl implements TrackDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Track> __insertionAdapterOfTrack;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTracksForPlaylist;

  public TrackDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrack = new EntityInsertionAdapter<Track>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tracks` (`fileUniqueId`,`playlistChannel`,`fileName`,`filePath`,`fileSize`,`mimeType`,`messageId`,`downloadedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Track entity) {
        statement.bindString(1, entity.getFileUniqueId());
        statement.bindString(2, entity.getPlaylistChannel());
        statement.bindString(3, entity.getFileName());
        statement.bindString(4, entity.getFilePath());
        statement.bindLong(5, entity.getFileSize());
        statement.bindString(6, entity.getMimeType());
        statement.bindLong(7, entity.getMessageId());
        statement.bindLong(8, entity.getDownloadedAt());
      }
    };
    this.__preparedStmtOfDeleteAllTracksForPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tracks WHERE playlistChannel = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertTrack(final Track track) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTrack.insert(track);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertTracks(final List<Track> tracks) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfTrack.insert(tracks);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAllTracksForPlaylist(final String playlistChannel) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTracksForPlaylist.acquire();
    int _argIndex = 1;
    _stmt.bindString(_argIndex, playlistChannel);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAllTracksForPlaylist.release(_stmt);
    }
  }

  @Override
  public List<Track> getTracksForPlaylist(final String playlistChannel) {
    final String _sql = "SELECT * FROM tracks WHERE playlistChannel = ? ORDER BY messageId ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, playlistChannel);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfFileUniqueId = CursorUtil.getColumnIndexOrThrow(_cursor, "fileUniqueId");
      final int _cursorIndexOfPlaylistChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistChannel");
      final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
      final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
      final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
      final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
      final int _cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(_cursor, "messageId");
      final int _cursorIndexOfDownloadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedAt");
      final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Track _item;
        final String _tmpFileUniqueId;
        _tmpFileUniqueId = _cursor.getString(_cursorIndexOfFileUniqueId);
        final String _tmpPlaylistChannel;
        _tmpPlaylistChannel = _cursor.getString(_cursorIndexOfPlaylistChannel);
        final String _tmpFileName;
        _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
        final String _tmpFilePath;
        _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
        final long _tmpFileSize;
        _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
        final String _tmpMimeType;
        _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
        final int _tmpMessageId;
        _tmpMessageId = _cursor.getInt(_cursorIndexOfMessageId);
        final long _tmpDownloadedAt;
        _tmpDownloadedAt = _cursor.getLong(_cursorIndexOfDownloadedAt);
        _item = new Track(_tmpFileUniqueId,_tmpPlaylistChannel,_tmpFileName,_tmpFilePath,_tmpFileSize,_tmpMimeType,_tmpMessageId,_tmpDownloadedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Track getTrackByFileId(final String fileUniqueId) {
    final String _sql = "SELECT * FROM tracks WHERE fileUniqueId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fileUniqueId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfFileUniqueId = CursorUtil.getColumnIndexOrThrow(_cursor, "fileUniqueId");
      final int _cursorIndexOfPlaylistChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistChannel");
      final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
      final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
      final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
      final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
      final int _cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(_cursor, "messageId");
      final int _cursorIndexOfDownloadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedAt");
      final Track _result;
      if (_cursor.moveToFirst()) {
        final String _tmpFileUniqueId;
        _tmpFileUniqueId = _cursor.getString(_cursorIndexOfFileUniqueId);
        final String _tmpPlaylistChannel;
        _tmpPlaylistChannel = _cursor.getString(_cursorIndexOfPlaylistChannel);
        final String _tmpFileName;
        _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
        final String _tmpFilePath;
        _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
        final long _tmpFileSize;
        _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
        final String _tmpMimeType;
        _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
        final int _tmpMessageId;
        _tmpMessageId = _cursor.getInt(_cursorIndexOfMessageId);
        final long _tmpDownloadedAt;
        _tmpDownloadedAt = _cursor.getLong(_cursorIndexOfDownloadedAt);
        _result = new Track(_tmpFileUniqueId,_tmpPlaylistChannel,_tmpFileName,_tmpFilePath,_tmpFileSize,_tmpMimeType,_tmpMessageId,_tmpDownloadedAt);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getAllFileIdsForPlaylist(final String playlistChannel) {
    final String _sql = "SELECT fileUniqueId FROM tracks WHERE playlistChannel = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, playlistChannel);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final String _item;
        _item = _cursor.getString(0);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getTrackCount() {
    final String _sql = "SELECT COUNT(*) FROM tracks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public long getTotalStorageUsed() {
    final String _sql = "SELECT COALESCE(SUM(fileSize), 0) FROM tracks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final long _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getLong(0);
      } else {
        _result = 0L;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public void deleteTracksByFileIds(final List<String> fileIds) {
    __db.assertNotSuspendingTransaction();
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("DELETE FROM tracks WHERE fileUniqueId IN (");
    final int _inputSize = fileIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
    int _argIndex = 1;
    for (String _item : fileIds) {
      _stmt.bindString(_argIndex, _item);
      _argIndex++;
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
