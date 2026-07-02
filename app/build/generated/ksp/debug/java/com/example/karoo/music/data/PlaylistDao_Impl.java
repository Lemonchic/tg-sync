package com.example.karoo.music.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlaylistDao_Impl implements PlaylistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Playlist> __insertionAdapterOfPlaylist;

  private final EntityDeletionOrUpdateAdapter<Playlist> __updateAdapterOfPlaylist;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlaylist;

  public PlaylistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaylist = new EntityInsertionAdapter<Playlist>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlists` (`channelUsername`,`displayName`,`channelId`,`lastSyncTimestamp`,`lastUpdateId`,`trackCount`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Playlist entity) {
        statement.bindString(1, entity.getChannelUsername());
        statement.bindString(2, entity.getDisplayName());
        statement.bindLong(3, entity.getChannelId());
        statement.bindLong(4, entity.getLastSyncTimestamp());
        statement.bindLong(5, entity.getLastUpdateId());
        statement.bindLong(6, entity.getTrackCount());
      }
    };
    this.__updateAdapterOfPlaylist = new EntityDeletionOrUpdateAdapter<Playlist>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `playlists` SET `channelUsername` = ?,`displayName` = ?,`channelId` = ?,`lastSyncTimestamp` = ?,`lastUpdateId` = ?,`trackCount` = ? WHERE `channelUsername` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Playlist entity) {
        statement.bindString(1, entity.getChannelUsername());
        statement.bindString(2, entity.getDisplayName());
        statement.bindLong(3, entity.getChannelId());
        statement.bindLong(4, entity.getLastSyncTimestamp());
        statement.bindLong(5, entity.getLastUpdateId());
        statement.bindLong(6, entity.getTrackCount());
        statement.bindString(7, entity.getChannelUsername());
      }
    };
    this.__preparedStmtOfDeletePlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlists WHERE channelUsername = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertPlaylist(final Playlist playlist) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfPlaylist.insert(playlist);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updatePlaylist(final Playlist playlist) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfPlaylist.handle(playlist);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deletePlaylist(final String channelUsername) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlaylist.acquire();
    int _argIndex = 1;
    _stmt.bindString(_argIndex, channelUsername);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeletePlaylist.release(_stmt);
    }
  }

  @Override
  public List<Playlist> getAllPlaylists() {
    final String _sql = "SELECT * FROM playlists ORDER BY displayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfChannelUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "channelUsername");
      final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
      final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
      final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncTimestamp");
      final int _cursorIndexOfLastUpdateId = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdateId");
      final int _cursorIndexOfTrackCount = CursorUtil.getColumnIndexOrThrow(_cursor, "trackCount");
      final List<Playlist> _result = new ArrayList<Playlist>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Playlist _item;
        final String _tmpChannelUsername;
        _tmpChannelUsername = _cursor.getString(_cursorIndexOfChannelUsername);
        final String _tmpDisplayName;
        _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
        final long _tmpChannelId;
        _tmpChannelId = _cursor.getLong(_cursorIndexOfChannelId);
        final long _tmpLastSyncTimestamp;
        _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
        final int _tmpLastUpdateId;
        _tmpLastUpdateId = _cursor.getInt(_cursorIndexOfLastUpdateId);
        final int _tmpTrackCount;
        _tmpTrackCount = _cursor.getInt(_cursorIndexOfTrackCount);
        _item = new Playlist(_tmpChannelUsername,_tmpDisplayName,_tmpChannelId,_tmpLastSyncTimestamp,_tmpLastUpdateId,_tmpTrackCount);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Playlist getPlaylistByChannel(final String channelUsername) {
    final String _sql = "SELECT * FROM playlists WHERE channelUsername = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, channelUsername);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfChannelUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "channelUsername");
      final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
      final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
      final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncTimestamp");
      final int _cursorIndexOfLastUpdateId = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdateId");
      final int _cursorIndexOfTrackCount = CursorUtil.getColumnIndexOrThrow(_cursor, "trackCount");
      final Playlist _result;
      if (_cursor.moveToFirst()) {
        final String _tmpChannelUsername;
        _tmpChannelUsername = _cursor.getString(_cursorIndexOfChannelUsername);
        final String _tmpDisplayName;
        _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
        final long _tmpChannelId;
        _tmpChannelId = _cursor.getLong(_cursorIndexOfChannelId);
        final long _tmpLastSyncTimestamp;
        _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
        final int _tmpLastUpdateId;
        _tmpLastUpdateId = _cursor.getInt(_cursorIndexOfLastUpdateId);
        final int _tmpTrackCount;
        _tmpTrackCount = _cursor.getInt(_cursorIndexOfTrackCount);
        _result = new Playlist(_tmpChannelUsername,_tmpDisplayName,_tmpChannelId,_tmpLastSyncTimestamp,_tmpLastUpdateId,_tmpTrackCount);
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
  public int getPlaylistCount() {
    final String _sql = "SELECT COUNT(*) FROM playlists";
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
