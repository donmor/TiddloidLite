/*
 * top.donmor.tiddloidlite.MainActivity <= [P|Tiddloid Lite]
 * Last modified: 18:18:25 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
	private RecyclerView rvWikiList;
	private TextView noWiki;
	private WikiListAdapter wikiListAdapter = null;
	private JSONObject db;

	// CONSTANT
	static final int TAKE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
	static final String
			KEY_NAME = "name",
			KEY_LBL = " — ",
			KEY_FAVICON = "favicon",
			KEY_ID = "id",
	//			KEY_INDEX = "index.html",
	DB_KEY_WIKI = "wiki",
			DB_KEY_COLOR = "color",
			DB_KEY_URI = "uri",
			KEY_SHORTCUT = "shortcut",
			DB_KEY_SUBTITLE = "subtitle",
			STR_EMPTY = "",
			TYPE_HTML = "text/html";
	private static final String
			DB_FILE_NAME = "data.json",
			KEY_APPLICATION_NAME = "application-name",
			KEY_CONTENT = "content",
	//			KEY_STORE_AREA = "storeArea",
//			KEY_PRE = "pre",
	KEY_URI_RATE = "market://details?id="
//	,
//			KEY_WIKI_TITLE = "$:/SiteTitle",
//			KEY_WIKI_SUBTITLE = "$:/SiteSubTitle",
//			KEY_WIKI_FAVICON = "$:/favicon.ico",
//			KEY_TITLE = "title"
			;
	private static final int REQUEST_OPEN = 42, REQUEST_CREATE = 43;
	/*TODO: 数据文件结构
	 *
	 * "30db5f9c-a776-464b-9c6e-3c96a33e350c" = {
	 * "id": "30db5f9c-a776-464b-9c6e-3c96a33e350c",	-- 唯一标识	//TODO: new structure
	 * "subtitle": "",	-- 副标题
	 * "uri": "content:\/\/com.android.externalstorage.documents\/document\/0B0B-2016%3A123.htm",	-- 文件Uri (content:// *2.0:file://*)
	 * "name": "My TiddlyWiki — a non-linear personal web notebook"	-- 标题
	 * "favicon": "*BASE64*"	-- 图标
	 * -- 以下为Tiddloid-2.0扩充 --
	 * "dir": "false"	-- 目录模式，仅content://
	 * },
	 *
	 * */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		setContentView(R.layout.activity_main);
		// 读取/初始化db，转换新格式
		try {
			db = readJson(this);
		} catch (Exception e) {
			e.printStackTrace();
			db = initJson(this);
			try {
//				db.put(DB_KEY_WIKI, new JSONArray());    //TODO: refactor
				db.put(DB_KEY_WIKI, new JSONObject());
				writeJson(this, db);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		trimDB115(this, db);
		// 加载UI
		onConfigurationChanged(getResources().getConfiguration());
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		noWiki = findViewById(R.id.t_noWiki);
//		try {
//			if (db.getJSONArray(DB_KEY_WIKI).length() == 0)
//				noWiki.setVisibility(View.VISIBLE);
//			else
//				noWiki.setVisibility(View.GONE);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		rvWikiList = findViewById(R.id.rvWikiList);
		rvWikiList.setLayoutManager(new LinearLayoutManager(this));
		try {
			wikiListAdapter = new WikiListAdapter(this, db);
			wikiListAdapter.setReloadListener(new WikiListAdapter.ReloadListener() {
				@Override
				public void onReloaded(int count) {
					noWiki.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
				}
			});
			wikiListAdapter.setOnItemClickListener(new WikiListAdapter.ItemClickListener() {
				// 点击打开
				@Override
				public void onItemClick(final String id) {
					try {
						JSONObject wa = db.getJSONObject(DB_KEY_WIKI).getJSONObject(id);
						Uri uri = Uri.parse(wa.getString(DB_KEY_URI));
						if (uri != null && new TWInfo(MainActivity.this, uri).isWiki) {
							getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
							if (!loadPage(id))
								Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
						} else {
							new AlertDialog.Builder(MainActivity.this)
									.setTitle(android.R.string.dialog_alert_title)
									.setMessage(R.string.confirm_to_auto_remove_wiki)
									.setNegativeButton(android.R.string.no, null)
									.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											removeWiki(id);
											//										try {
											//											db.getJSONObject("DKW2").remove(id);
											////											db.getJSONArray(DB_KEY_WIKI).remove(position);
											//											writeJson(MainActivity.this, db);
											//											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
											//												revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
											//										} catch (Exception e) {
											//											e.printStackTrace();
											//										}
											//										MainActivity.this.onResume();
										}
									}).show();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
					}
				}

				// 长按属性
				@SuppressLint("QueryPermissionsNeeded")
				@Override
				public void onItemLongClick(final String id) {    //TODO: refactor
					try {
						JSONObject wl = db.getJSONObject(DB_KEY_WIKI), wa = wl.getJSONObject(id);
						Uri uri = Uri.parse(wa.getString(DB_KEY_URI));
						final String name = wa.optString(KEY_NAME), sub = wa.optString(DB_KEY_SUBTITLE);
						Drawable icon = null;
						byte[] b = Base64.decode(wa.optString(KEY_FAVICON), Base64.NO_PADDING);
						final Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							icon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_description);
							if (favicon != null) try {
								icon = new BitmapDrawable(getResources(), favicon);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						//					final IconCompat iconCompat = favicon != null ? IconCompat.createWithBitmap(favicon) : IconCompat.createWithResource(MainActivity.this, R.drawable.ic_shortcut);
						TextView view = new TextView(MainActivity.this);
						DocumentFile file = DocumentFile.fromSingleUri(MainActivity.this, uri);
						String fn = file != null ? file.getName() : null, provider = getString(R.string.unknown);
						// 获取来源名
						PackageManager pm = getPackageManager();
						for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
							if (uri.getAuthority().startsWith(info.packageName)) {
								provider = pm.getApplicationLabel(info).toString();
								break;
							}
						}
						// 显示属性
						CharSequence s = getString(R.string.provider)
								+ provider
								//							+ uri.getAuthority()
								+ '\n'
								+ getString(R.string.pathDir)
								+ Uri.decode(uri.getLastPathSegment())
								+ '\n'
								+ getString(R.string.filename)
								+ (fn != null && fn.length() > 0 ? fn : getString(R.string.unknown));
						view.setText(s);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
							view.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
						AlertDialog wikiConfigDialog = new AlertDialog.Builder(MainActivity.this)
								.setTitle(name)
								.setIcon(icon)
								.setPositiveButton(android.R.string.ok, null)
								//							.setOnDismissListener(new DialogInterface.OnDismissListener() {
								//								@Override
								//								public void onDismiss(DialogInterface dialog) {
								//									MainActivity.this.onResume();
								//								}
								//							})
								.setNegativeButton(R.string.remove_wiki, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.dismiss();
										// 移除确认
										AlertDialog removeWikiConfirmationDialog = new AlertDialog.Builder(MainActivity.this)
												.setTitle(android.R.string.dialog_alert_title)
												.setMessage(R.string.confirm_to_remove_wiki)
												.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialogInterface, int i) {
														MainActivity.this.onResume();
													}
												})
												.setNeutralButton(R.string.delete_the_html_file, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialogInterface, int i) {
														removeWiki(id, true);
														//													try {
														//														wl.remove(id);
														////														db.getJSONArray(DB_KEY_WIKI).remove(position);
														//														writeJson(MainActivity.this, db);
														//														DocumentsContract.deleteDocument(getContentResolver(), uri);
														//														if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
														//															revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
														//														Toast.makeText(MainActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
														//													} catch (Exception e) {
														//														e.printStackTrace();
														//														Toast.makeText(MainActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
														//													}
														//													MainActivity.this.onResume();
													}
												})
												.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														removeWiki(id);
														//													try {
														//														wl.remove(id);
														////														db.getJSONArray(DB_KEY_WIKI).remove(position);
														//														writeJson(MainActivity.this, db);
														//														if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
														//															revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
														//													} catch (Exception e) {
														//														e.printStackTrace();
														//														Toast.makeText(MainActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
														//													}
														//													MainActivity.this.onResume();
													}
												})
												.create();
										removeWikiConfirmationDialog.show();
									}
								})
								.setNeutralButton(R.string.create_shortcut, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										try {
											// 快捷方式
											Bundle bu = new Bundle();
											bu.putString(KEY_ID, id);
											bu.putBoolean(KEY_SHORTCUT, true);
											Intent in = new Intent(MainActivity.this, TWEditorWV.class).putExtras(bu).setAction(Intent.ACTION_MAIN);
											if (ShortcutManagerCompat.isRequestPinShortcutSupported(MainActivity.this)) {
												ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(MainActivity.this, id)
														.setShortLabel(name)
														.setLongLabel(name + (sub != null ? (KEY_LBL + sub) : MainActivity.STR_EMPTY))
														.setIcon(favicon != null ? IconCompat.createWithBitmap(favicon) : IconCompat.createWithResource(MainActivity.this, R.drawable.ic_shortcut))
														.setIntent(in)
														.build();
												if (ShortcutManagerCompat.requestPinShortcut(MainActivity.this, shortcut, null))
													Toast.makeText(MainActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
												else throw new Exception();
											}
										} catch (Exception e) {
											e.printStackTrace();
											Toast.makeText(MainActivity.this, R.string.shortcut_failed, Toast.LENGTH_SHORT).show();
										}
									}
								})
								.create();
						int m = Math.round(20 * getResources().getDisplayMetrics().density + 0.5f);
						wikiConfigDialog.setView(view, m, 0, m, 0);
						wikiConfigDialog.show();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
					}
				}
			});
			noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private Boolean loadPage(String id) {
		try {
//			String vid = null;
//			for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//				if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID).equals(id)) {
//					vid = id;
//					break;
//				}
//			}
			if (db.getJSONObject(DB_KEY_WIKI).has(id)) {
				Bundle bu = new Bundle();
				bu.putString(KEY_ID, id);
				Intent in = new Intent().putExtras(bu).setClass(MainActivity.this, TWEditorWV.class);
				startActivity(in);
			} else throw new FileNotFoundException();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void removeWiki(String id) {
		removeWiki(id, false);
	}

	private void removeWiki(String id, boolean del) {
		try {
			JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
			Uri uri = Uri.parse(wl.getJSONObject(id).getString(DB_KEY_URI));
			wl.remove(id);
			writeJson(MainActivity.this, db);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				revokeUriPermission(getPackageName(), uri, TAKE_FLAGS);
			if (del) {
				DocumentsContract.deleteDocument(getContentResolver(), uri);
				Toast.makeText(MainActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
		}
		onResume();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	// 菜单栏
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_new) {
			startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), REQUEST_CREATE);
		} else if (id == R.id.action_import) {
			startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), REQUEST_OPEN);
		} else if (id == R.id.action_about) {
			SpannableString spannableString = new SpannableString(getString(R.string.about));
			Linkify.addLinks(spannableString, Linkify.ALL);
			AlertDialog aboutDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.action_about)
					.setMessage(spannableString)
					.setPositiveButton(android.R.string.ok, null)
					.setNeutralButton(R.string.market, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(KEY_URI_RATE + getPackageName())));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					})
					.show();
			((TextView) aboutDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				((TextView) aboutDialog.findViewById(android.R.id.message)).setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		}
		return super.onOptionsItemSelected(item);
	}

	// SAF处理
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (resultCode == Activity.RESULT_OK && resultData != null) {
			Uri uri = resultData.getData();
			if (uri != null)
				switch (requestCode) {
					case REQUEST_CREATE:    // 新建
						createWiki(uri);
						break;
					case REQUEST_OPEN:    // 导入
						importWiki(uri);
						break;
				}
		}
	}

	private void createWiki(final Uri uri) {
		// 对话框等待
		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.please_wait));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCanceledOnTouchOutside(false);
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream is = null;
				OutputStream os = null;
				boolean interrupted = false;
				boolean iNet = false;
				try {
					// 下载模板
					os = getContentResolver().openOutputStream(uri);
					if (os != null) {
						URL url;
						url = new URL(getString(R.string.template_repo));
						HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
						if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
							urlConnection.setSSLSocketFactory(new TLSSocketFactory());
						urlConnection.connect();
						iNet = true;
						is = urlConnection.getInputStream();
						int length;
						byte[] bytes = new byte[4096];
						while ((length = is.read(bytes)) > -1) {
							os.write(bytes, 0, length);
							if (Thread.currentThread().isInterrupted()) {
								interrupted = true;
								break;
							}
						}
						os.flush();
						os.close();
						os = null;
						if (!interrupted) {
							TWInfo info = new TWInfo(MainActivity.this, uri);
							if (!info.isWiki)
								throw new IOException();
							progressDialog.dismiss();
							String id = null;
							try {
								// 查重

								JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
								boolean exist = false;
								Iterator<String> iterator = wl.keys();
								JSONObject w = null;
								while (iterator.hasNext()) {
									id = iterator.next();
									w = wl.getJSONObject(id);
									exist = uri.toString().equals(w.getString(DB_KEY_URI));
									if (exist) break;
								}
//								for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//									w = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i);
//									if (w.getString(DB_KEY_URI).equals(uri.toString())) {
//										exist = true;
//										id = w.getString(KEY_ID);
//										break;
//									}
//								}
								if (exist) {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
										}
									});
//									new File(getDir(MainActivity.KEY_FAVICON, MODE_PRIVATE), id).delete();
								} else {
									id = genId();
									w = new JSONObject();
									w.put(KEY_ID, id);
									wl.put(id, w);
//									db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
								}
								w.put(KEY_NAME, getString(R.string.tiddlywiki));
//								w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getString(R.string.tiddlywiki));
								w.put(DB_KEY_SUBTITLE, STR_EMPTY);
//								w.put(DB_KEY_SUBTITLE, (info.subtitle != null && info.subtitle.length() > 0) ? info.subtitle : STR_EMPTY);
								w.put(DB_KEY_URI, uri.toString());
//								updateIcon(MainActivity.this, info.favicon, id);
								if (!MainActivity.writeJson(MainActivity.this, db))
									throw new Exception();
								getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
							} catch (Exception e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
									}
								});
							}
//							runOnUiThread(new Runnable() {
//								@Override
//								public void run() {
//									MainActivity.this.onResume();
//								}
//							});
							if (!loadPage(id))
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
									}
								});
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					progressDialog.dismiss();
					try {
						DocumentsContract.deleteDocument(getContentResolver(), uri);
					} catch (Exception e1) {
						e.printStackTrace();
					}
					final int fid = iNet ? R.string.failed_creating_file : R.string.no_internet;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, fid, Toast.LENGTH_SHORT).show();
						}
					});
				} finally {
					if (is != null)
						try {
							is.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					if (os != null)
						try {
							os.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					if (interrupted) try {//TODO
						DocumentsContract.deleteDocument(getContentResolver(), uri);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, R.string.cancelled, Toast.LENGTH_SHORT).show();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				progressDialog.cancel();
			}
		});
		progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				thread.start();
			}
		});
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				thread.interrupt();
			}
		});
		progressDialog.show();
	}

	private void importWiki(Uri uri) {
		String id = genId();
		TWInfo info = new TWInfo(MainActivity.this, uri);
		if (info.isWiki) {
			try {
				JSONObject wl = db.getJSONObject(DB_KEY_WIKI);
				boolean exist = false;
				Iterator<String> iterator = wl.keys();
				JSONObject w;
				while (iterator.hasNext()) {
					id = iterator.next();
					w = wl.getJSONObject(id);
					exist = uri.toString().equals(w.getString(DB_KEY_URI));
					if (exist) break;
				}

//				for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//					if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_URI).equals(uri.toString())) {
//						exist = true;
//						id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
//						break;
//					}
//				}
				if (exist) {
					Toast.makeText(MainActivity.this, R.string.wiki_already_exists, Toast.LENGTH_SHORT).show();
				} else {
					w = new JSONObject();
					w.put(KEY_NAME, getString(R.string.tiddlywiki));
//					w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getString(R.string.tiddlywiki));
					w.put(DB_KEY_SUBTITLE, STR_EMPTY);
//					w.put(DB_KEY_SUBTITLE, (info.subtitle != null && info.subtitle.length() > 0) ? info.subtitle : STR_EMPTY);
//					w.put(KEY_ID, id);
					w.put(DB_KEY_URI, uri.toString());
					wl.put(id, w);
//					db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
//					updateIcon(this, info.favicon, id);
				}
				if (!MainActivity.writeJson(MainActivity.this, db))
					throw new Exception();
				getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
			}
//			MainActivity.this.onResume();
			if (!loadPage(id))
				Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(MainActivity.this, R.string.not_a_wiki, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			db = readJson(this);
			wikiListAdapter.reload(db);
			rvWikiList.setAdapter(wikiListAdapter);
			noWiki.setVisibility(wikiListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Window w = getWindow();
			int color = getColor(R.color.design_default_color_primary);
			w.setStatusBarColor(color);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				w.setNavigationBarColor(color);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES)
				Objects.requireNonNull(w.getInsetsController()).setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS | WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS | WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
			else
				w.getDecorView().setSystemUiVisibility((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : View.SYSTEM_UI_FLAG_VISIBLE) : View.SYSTEM_UI_FLAG_VISIBLE);
//			w.getDecorView().setSystemUiVisibility((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : View.SYSTEM_UI_FLAG_VISIBLE);
		}
	}

	static JSONObject initJson(Context context) {
		byte[] b;
		InputStream is = null;
		JSONObject jsonObject = new JSONObject();
		try {
			File ext = context.getExternalFilesDir(null);
			if (ext == null) return jsonObject;
			File[] exd = ext.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return DB_FILE_NAME.equals(name);
				}
			});
			if (exd == null) return jsonObject;
			is = new FileInputStream(exd[0]);
			b = new byte[is.available()];
			if (is.read(b) < 0) throw new Exception();
			jsonObject = new JSONObject(new String(b));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		return jsonObject;
	}

	static JSONObject readJson(Context context) throws Exception {
		byte[] b;
		InputStream is = context.openFileInput(DB_FILE_NAME);
		b = new byte[is.available()];
		if (is.read(b) < 0) throw new Exception();
		JSONObject jsonObject = new JSONObject(new String(b));
		is.close();


		return jsonObject;
	}

	static boolean writeJson(Context context, JSONObject vdb) {
		boolean v;
		OutputStream os = null;
		try {
			os = context.openFileOutput(DB_FILE_NAME, MODE_PRIVATE);
			byte[] b = vdb.toString(2).getBytes();
			os.write(b);
			os.flush();
			v = true;
		} catch (Exception e) {
			e.printStackTrace();
			v = false;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		return v;
	}

	static void exportJson(Context context, JSONObject vdb) {
		OutputStream os = null;
		try {
			File ext = context.getExternalFilesDir(null);
			if (ext == null) return;
			os = new FileOutputStream(ext.getPath() + File.separator + DB_FILE_NAME);
//			os = context.openFileOutput(DB_FILE_NAME, MODE_PRIVATE);
			byte[] b = vdb.toString(2).getBytes();
			os.write(b);
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

//	static void updateIcon(Context context, Bitmap icon, String id) {
//		File fi = new File(context.getDir(MainActivity.KEY_FAVICON, MODE_PRIVATE), id);
//		if (icon != null) {
//			OutputStream os = null;
//			try {
//				os = new FileOutputStream(fi);
//				icon.compress(Bitmap.CompressFormat.PNG, 100, os);
//				os.flush();
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (os != null)
//					try {
//						os.close();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//			}
//		} else fi.delete();
//	}

	private static String genId() {
		return UUID.randomUUID().toString();
	}

	static class TWInfo {
		boolean isWiki;
//		private String title = null, subtitle = null;
//		private Bitmap favicon = null;

		TWInfo(final Context context, Uri uri) {
			InputStream is = null;
			try {
				is = new BufferedInputStream(Objects.requireNonNull(context.getContentResolver().openInputStream(uri)));
				Document doc = Jsoup.parse(is, StandardCharsets.UTF_8.name(), uri.toString());
				Element an = doc.selectFirst(new Evaluator.AttributeKeyPair(KEY_NAME, KEY_APPLICATION_NAME) {
					@Override
					public boolean matches(Element root, Element element) {
						return context.getString(R.string.tiddlywiki).equals(element.attr(KEY_CONTENT));
					}
				});
				isWiki = an != null;
//				if (isWiki = an != null) {
//					Element ti = doc.getElementsByTag(KEY_TITLE).first();
//					title = ti != null ? ti.html() : title;
//					Element sa = doc.getElementsByAttributeValue(KEY_ID, KEY_STORE_AREA).first();
//					Element t1 = sa.getElementsByAttributeValue(KEY_TITLE, KEY_WIKI_TITLE).first();
//					Element t2 = sa.getElementsByAttributeValue(KEY_TITLE, KEY_WIKI_SUBTITLE).first();
//					title = t1 != null ? t1.getElementsByTag(KEY_PRE).first().html() : title;
//					subtitle = t2 != null ? t2.getElementsByTag(KEY_PRE).first().html() : subtitle;
//					Element fi = sa.getElementsByAttributeValue(KEY_TITLE, KEY_WIKI_FAVICON).first();
//					byte[] b = fi != null ? Base64.decode(fi.getElementsByTag(KEY_PRE).first().html(), Base64.NO_PADDING) : new byte[0];
//					favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
//				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	static void trimDB115(Context context, JSONObject db) {
		try {
			JSONArray wl = db.optJSONArray(DB_KEY_WIKI);
			if (wl == null) return;
//			System.out.println(db.has("DKW2"));
//			if (db.has("DKW2")) return;
			JSONObject wl2 = new JSONObject();
			for (int i = 0; i < wl.length(); i++) {
				JSONObject wiki = new JSONObject(), w0 = wl.optJSONObject(i);
				if (w0 == null) continue;
				wiki.put(KEY_NAME, w0.optString(KEY_NAME, null));
				wiki.put(DB_KEY_SUBTITLE, w0.optString(DB_KEY_SUBTITLE, null));
				wiki.put(DB_KEY_URI, w0.optString(DB_KEY_URI, null));
				wl2.put(wl.getJSONObject(i).getString(KEY_ID), wiki);
			}
			db.remove(DB_KEY_WIKI);
			db.put(DB_KEY_WIKI, wl2);
//			db.put("DKW2", wl2);
			writeJson(context, db);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}