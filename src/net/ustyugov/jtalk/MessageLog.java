/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk;

import net.ustyugov.jtalk.db.JTalkProvider;
import net.ustyugov.jtalk.db.MessageDbHelper;
import net.ustyugov.jtalk.service.JTalkService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import org.jivesoftware.smack.util.StringUtils;

import java.util.List;

public class MessageLog {
	
	public static void writeMessage(String account, String jid, MessageItem message) {
        JTalkService service = JTalkService.getInstance();
        List<MessageItem> list = service.getMessageList(account, jid);
        if (message.getType() == MessageItem.Type.status) {
            if (service.getActiveChats(account).contains(jid)) list.add(message);
        } else {
            if (!service.getActiveChats(account).contains(jid)) service.addActiveChat(account, jid);
            list.add(message);
        }
        service.setMessageList(account, jid, list);

        ContentValues values = new ContentValues();
        values.put(MessageDbHelper.TYPE, message.getType().name());
        values.put(MessageDbHelper.JID, jid);
        values.put(MessageDbHelper.ID, message.getId());
        values.put(MessageDbHelper.STAMP, message.getTime());
        values.put(MessageDbHelper.NICK, message.getName());
        values.put(MessageDbHelper.BODY, message.getBody());
        values.put(MessageDbHelper.COLLAPSED, false);
        values.put(MessageDbHelper.RECEIVED, message.isReceived() ? "true" : "false");
        values.put(MessageDbHelper.FORM, "NULL");
        values.put(MessageDbHelper.BOB, "NULL");
        service.getContentResolver().insert(JTalkProvider.CONTENT_URI, values);

        service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));
    }
	
	public static void writeMucMessage(String account, final String group, final String nick, final MessageItem message) {
        JTalkService service = JTalkService.getInstance();
        List<MessageItem> list = service.getMessageList(account, group);
        list.add(message);
        service.setMessageList(account, group, list);

        ContentValues values = new ContentValues();
        values.put(MessageDbHelper.TYPE, message.getType().name());
        values.put(MessageDbHelper.JID, group);
        values.put(MessageDbHelper.ID, message.getId());
        values.put(MessageDbHelper.STAMP, message.getTime());
        values.put(MessageDbHelper.NICK, nick);
        values.put(MessageDbHelper.BODY, message.getBody());
        values.put(MessageDbHelper.COLLAPSED, false);
        values.put(MessageDbHelper.RECEIVED, message.isReceived() ? "true" : "false");
        values.put(MessageDbHelper.FORM, "NULL");
        values.put(MessageDbHelper.BOB, "NULL");
        service.getContentResolver().insert(JTalkProvider.CONTENT_URI, values);

        service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", group));
	}
	
	public static void editMessage(final String account, final String jid, final String rid, final String text) {
		final JTalkService service = JTalkService.getInstance();
		new Thread() {
			@Override
			public void run() {
                Cursor cursor = service.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + jid + "' AND id = '" + rid + "'", null, MessageDbHelper._ID);
                if (cursor != null && cursor.getCount() > 0 && text != null && text.length() > 0) {
                    cursor.moveToLast();
                    String _id = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                    String id = cursor.getString(cursor.getColumnIndex(MessageDbHelper.ID));
                    String nick = cursor.getString(cursor.getColumnIndex(MessageDbHelper.NICK));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    String received = cursor.getString(cursor.getColumnIndex(MessageDbHelper.RECEIVED));
                    cursor.close();

                    ContentValues values = new ContentValues();
                    values.put(MessageDbHelper.TYPE, type);
                    values.put(MessageDbHelper.JID, jid);
                    values.put(MessageDbHelper.ID, id);
                    values.put(MessageDbHelper.STAMP, stamp);
                    values.put(MessageDbHelper.NICK, nick);
                    values.put(MessageDbHelper.BODY, text);
                    values.put(MessageDbHelper.COLLAPSED, false);
                    values.put(MessageDbHelper.RECEIVED, received);
                    values.put(MessageDbHelper.FORM, "NULL");
                    values.put(MessageDbHelper.BOB, "NULL");

                    service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, "_ID = '" + _id + "'", null);

                    List<MessageItem> list = service.getMessageList(account, jid);
                    for (MessageItem item : list) {
                        if (item.getId().equals(rid)) {
                            item.setBody(text);
                            item.setEdited(true);
                        }
                    }
                    service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", jid));
                }
			}
		}.start();
	}

    public static void editMucMessage(final String account, final String from, final String rid, final String text) {
        final String group = StringUtils.parseBareAddress(from);
        final String nick = StringUtils.parseResource(from);
        final JTalkService service = JTalkService.getInstance();
        new Thread() {
            @Override
            public void run() {
                Cursor cursor = service.getContentResolver().query(JTalkProvider.CONTENT_URI, null, "jid = '" + group + "' AND id = '" + rid + "'", null, MessageDbHelper._ID);
                if (cursor != null && cursor.getCount() > 0 && text != null && text.length() > 0) {
                    cursor.moveToLast();
                    String _id = cursor.getString(cursor.getColumnIndex(MessageDbHelper._ID));
                    String type = cursor.getString(cursor.getColumnIndex(MessageDbHelper.TYPE));
                    String stamp = cursor.getString(cursor.getColumnIndex(MessageDbHelper.STAMP));
                    cursor.close();

                    ContentValues values = new ContentValues();
                    values.put(MessageDbHelper.TYPE, type);
                    values.put(MessageDbHelper.JID, group);
                    values.put(MessageDbHelper.ID, rid);
                    values.put(MessageDbHelper.STAMP, stamp);
                    values.put(MessageDbHelper.NICK, nick);
                    values.put(MessageDbHelper.BODY, text);
                    values.put(MessageDbHelper.COLLAPSED, false);
                    values.put(MessageDbHelper.RECEIVED, false);
                    values.put(MessageDbHelper.FORM, "NULL");
                    values.put(MessageDbHelper.BOB, "NULL");

                    service.getContentResolver().update(JTalkProvider.CONTENT_URI, values, "_ID = '" + _id + "'", null);

                    List<MessageItem> list = service.getMessageList(account, group);
                    for (MessageItem item : list) {
                        String msgID = item.getId();
                        if (rid.equals(msgID)) {
                            item.setBody(text);
                            item.setEdited(true);
                        }
                    }
                    service.sendBroadcast(new Intent(Constants.NEW_MESSAGE).putExtra("jid", group));
                }
            }
        }.start();
    }
}
