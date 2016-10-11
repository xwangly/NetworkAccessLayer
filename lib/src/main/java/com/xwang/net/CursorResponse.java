package com.xwang.net;

/**
 * Created by xwangly on 2015/12/22.
 * Network response cursor interface.
 */
public interface CursorResponse {
    int getErrorCode();

    boolean isError();

    String getErrorMsg();

    int getCount();

    int getPosition();

    boolean moveToPosition(int position);

    boolean moveToFirst();

    boolean moveToLast();

    boolean moveToNext();

    boolean moveToPrevious();

    boolean isFirst();

    boolean isLast();

    int getColumnIndex(String columnName);

    String getColumnName(int columnIndex);

    String[] getColumnNames();

    int getColumnCount();

    String getString(int columnIndex);

    String getString(String columnName);

    int getInt(int columnIndex);

    int getInt(String columnName);
}
