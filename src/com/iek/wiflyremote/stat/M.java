package com.iek.wiflyremote.stat;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.iek.wiflyremote.data.LocalDb;

public class M {
	private static M m = new M();
	private String host;
	private int port;
	// private Board board;
	// private LocalDb localdb;
	private SQLiteDatabase db;
	private List<Observer> boardRespObservers = new ArrayList<Observer>();
	private Socket globalSocket;
	private LocalDb localdb;
	private Runnable listenThr = new Runnable() {

		@Override
		public void run() {
			InputStream in;
			try {
				in = getGlobalSocket().getInputStream();
				byte[] buff = new byte[1024];
				int bread = 0;

				while ((bread = in.read(buff)) != -1) {
					final ByteArrayOutputStream ostream = new ByteArrayOutputStream(
							1024);
					ostream.write(buff, 0, bread);
					try {
						final String s = ostream.toString("UTF-8").replace(
								"\n", "");
						Log.i("SVRRESP", s);
						for (Observer obs : boardRespObservers) {
							obs.update(null, s);
						}
					} catch (UnsupportedEncodingException e) {
						Log.e("SVRRESP", e.getMessage());
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Socket getGlobalSocket() {
		return globalSocket;
	}

	public void setGlobalSocket(Socket globalSocket) {
		this.globalSocket = globalSocket;
	}

	private M() {
		// board = new Board();
	}

	public static M m() {
		return m;
	}

	public static class catalog {
		public static Map<String, String> settings;
	}

	public void connect(final Observer observer) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (M.m().getGlobalSocket() == null
							|| !M.m().getGlobalSocket().isConnected()) {
						M.m().setGlobalSocket(new Socket());
						getGlobalSocket().connect(
								new InetSocketAddress(M.m().getHost(), M.m()
										.getPort()), 3000);
						if (observer != null) {
							observer.update(null, "ok");
						}
						new Thread(listenThr).start();
					} else {
						if (observer != null) {
							observer.update(null, "ok");
						}
					}
				} catch (Exception e) {
					Log.i("CONNECT",
							e.getMessage() == null ? ":(" : e.getMessage());
					if (observer != null) {
						observer.update(null, "no");
					}
				}
			}
		}).start();
	}

	public void sendMessage(final Observer observer, final String msg) {
		new Thread(new Runnable() {

			@Override
			public void run() {

				PrintWriter pw;
				try {
					pw = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(
									globalSocket.getOutputStream())));

					pw.println(msg);
					pw.flush();
					if (observer != null) {
						observer.update(null, msg);
					}
					Log.i("SEND", msg);
				} catch (Exception e) {
					Log.e("SEND",
							e.getMessage() == null ? ":(" : e.getMessage());
				}
			}
		}).start();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void addBoardRespObserver(Observer observer) {
		if (observer != null) {
			boardRespObservers.add(observer);
		}
	}

	public void delBoardRespObserver(Observer observer) {
		if (observer != null) {
			boardRespObservers.remove(observer);
		}
	}

	public void clearBoardRespObserver() {
		this.boardRespObservers.clear();
	}

	// public Board getBoard() {
	// return board;
	// }
	//
	// public void setBoard(Board board) {
	// this.board = board;
	// }
	//
	public LocalDb getLocaldb() {
		return localdb;
	}

	public void setLocaldb(LocalDb localdb) {
		this.localdb = localdb;
		this.db = localdb.getWritableDatabase();
		localdb.onCreate(db);
	}

	// public void loadCatalogs() {
	// catalog.settings = new HashMap<String, String>();
	// List<CatRow> l = getLocaldb().selectCat("settings");
	// for (CatRow c : l) {
	// catalog.settings.put(c.getName(), c.getValue());
	// }
	//
	// }
}
