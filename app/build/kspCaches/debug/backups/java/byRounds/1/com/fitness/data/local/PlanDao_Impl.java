package com.fitness.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlanDao_Impl implements PlanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlanEntity> __insertionAdapterOfPlanEntity;

  private final SharedSQLiteStatement __preparedStmtOfArchiveCurrentPlans;

  public PlanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlanEntity = new EntityInsertionAdapter<PlanEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `training_plans` (`id`,`name`,`exercisesJson`,`isCurrent`,`version`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlanEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getExercisesJson());
        final int _tmp = entity.isCurrent() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getVersion());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfArchiveCurrentPlans = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE training_plans SET isCurrent = 0 WHERE isCurrent = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlan(final PlanEntity plan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlanEntity.insert(plan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlan(final PlanEntity newPlan, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> PlanDao.DefaultImpls.updatePlan(PlanDao_Impl.this, newPlan, __cont), $completion);
  }

  @Override
  public Object archiveCurrentPlans(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfArchiveCurrentPlans.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfArchiveCurrentPlans.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getCurrentPlan(final Continuation<? super PlanEntity> $completion) {
    final String _sql = "SELECT * FROM training_plans WHERE isCurrent = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlanEntity>() {
      @Override
      @Nullable
      public PlanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExercisesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "exercisesJson");
          final int _cursorIndexOfIsCurrent = CursorUtil.getColumnIndexOrThrow(_cursor, "isCurrent");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PlanEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExercisesJson;
            _tmpExercisesJson = _cursor.getString(_cursorIndexOfExercisesJson);
            final boolean _tmpIsCurrent;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCurrent);
            _tmpIsCurrent = _tmp != 0;
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new PlanEntity(_tmpId,_tmpName,_tmpExercisesJson,_tmpIsCurrent,_tmpVersion,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPlanForTimestamp(final long timestamp,
      final Continuation<? super PlanEntity> $completion) {
    final String _sql = "SELECT * FROM training_plans WHERE createdAt <= ? ORDER BY createdAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, timestamp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlanEntity>() {
      @Override
      @Nullable
      public PlanEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExercisesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "exercisesJson");
          final int _cursorIndexOfIsCurrent = CursorUtil.getColumnIndexOrThrow(_cursor, "isCurrent");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final PlanEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExercisesJson;
            _tmpExercisesJson = _cursor.getString(_cursorIndexOfExercisesJson);
            final boolean _tmpIsCurrent;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCurrent);
            _tmpIsCurrent = _tmp != 0;
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new PlanEntity(_tmpId,_tmpName,_tmpExercisesJson,_tmpIsCurrent,_tmpVersion,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllPlans(final Continuation<? super List<PlanEntity>> $completion) {
    final String _sql = "SELECT * FROM training_plans ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PlanEntity>>() {
      @Override
      @NonNull
      public List<PlanEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExercisesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "exercisesJson");
          final int _cursorIndexOfIsCurrent = CursorUtil.getColumnIndexOrThrow(_cursor, "isCurrent");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<PlanEntity> _result = new ArrayList<PlanEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlanEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExercisesJson;
            _tmpExercisesJson = _cursor.getString(_cursorIndexOfExercisesJson);
            final boolean _tmpIsCurrent;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCurrent);
            _tmpIsCurrent = _tmp != 0;
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new PlanEntity(_tmpId,_tmpName,_tmpExercisesJson,_tmpIsCurrent,_tmpVersion,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
