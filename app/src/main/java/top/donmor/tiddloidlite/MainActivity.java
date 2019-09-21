/*
 * top.donmor.tiddloidlite.MainActivity <= [P|Tiddloid Lite]
 * Last modified: 18:18:25 2019/05/10
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

//import androidx.core.widget.CursorAdapter;
//import androidx.core.widget.SwipeRefreshLayout;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.Toolbar;
//import org.mozilla.javascript.Scriptable;
//import top.donmor.tiddloid.utils.NoLeakHandler;
//import top.donmor.tiddloid.utils.TLSSocketFactory;
//import com.github.donmor.filedialog.lib.FileDialog;
//import com.github.donmor.filedialog.lib.FileDialogFilter;

public class MainActivity extends AppCompatActivity {
	private RecyclerView rvWikiList;
	private TextView noWiki;
	private WikiListAdapter wikiListAdapter;
	private JSONObject db;

	// CONSTANT
//	static final FileDialogFilter[] HTML_FILTERS = {new FileDialogFilter(".html;.htm", new String[]{".html", ".htm"})};
	static final int TAKE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
	static final String
//			SCHEME_BLOB_B64 = "blob-b64",
//			BACKUP_DIRECTORY_PATH_PREFIX = "_backup",
			KEY_NAME = "name",
			KEY_FAVICON = "favicon",
			KEY_ID = "id",
//			KEY_URL = "url",
			DB_FILE_NAME = "data.json",
//			DB_KEY_SHOW_HIDDEN = "showHidden",
//			DB_KEY_LAST_DIR = "lastDir",
			DB_KEY_WIKI = "wiki",
//			DB_KEY_PATH = "path",
			DB_KEY_URI = "uri";
//			DB_KEY_BACKUP = "backup";
	private static final String
//			DB_KEY_CSE = "customSearchEngine",
//			DB_KEY_SEARCH_ENGINE = "searchEngine",
			KEY_APPLICATION_NAME = "application-name",
			KEY_LBL = " — ",
//			KEY_DOWNLOAD = "download",
			KEY_CONTENT = "content",
			KEY_VERSION = "version",
			KEY_VERSION_AREA = "versionArea",
			KEY_TITLE = "title",
			TYPE_HTML = "text/html",
//			SE_GOOGLE = "Google",
//			SE_BING = "Bing",
//			SE_BAIDU = "Baidu",
//			SE_SOGOU = "Sogou",
//			SE_CUSTOM = "Custom",
			PREF_VER_1 = "var version = ",
			PREF_VER_2 = "};",
			PREF_VER_3 = "new Date(",
			PREF_VER_4 = ")",
//			PREF_S = "%s",
//			PREF_SU = "#content#",
//			SCH_EX_HTTP = "http://",
//			TEMPLATE_FILE_NAME = "template.html",
//			MIME_HTML = "text/html",
//			CHARSET_DEFAULT = "UTF-8",
			CLASS_MENU_BUILDER = "MenuBuilder",
			METHOD_SET_OPTIONAL_ICONS_VISIBLE = "setOptionalIconsVisible";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		setContentView(R.layout.activity_main);
//		File templateOnStart = new File(getFilesDir(), TEMPLATE_FILE_NAME);
//		if (!templateOnStart.exists() || !(new TWInfo(MainActivity.this, templateOnStart).isWiki)) {
//			final ProgressDialog progressDialog = new ProgressDialog(this);
//			progressDialog.setMessage(getResources().getString(R.string.please_wait));
//			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//			progressDialog.setCancelable(false);
//			progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//				@Override
//				public void onShow(DialogInterface dialog) {
//					wGet(MainActivity.this, Uri.parse(getResources().getString(R.string.template_repo)), new File(getFilesDir(), TEMPLATE_FILE_NAME), true, true, new DownloadChecker() {
//						@Override
//						public boolean checkNg(File file) {
//							return !(new TWInfo(MainActivity.this, file).isWiki);
//						}
//					}, new OnDownloadCompleteListener() {
//						@Override
//						public void onDownloadComplete(File file) {
//							if (file.exists())
//								Toast.makeText(MainActivity.this, R.string.download_complete, Toast.LENGTH_SHORT).show();
//							else
//								Toast.makeText(MainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
//							progressDialog.dismiss();
//						}
//
//						@Override
//						public void onDownloadFailed() {
//							Toast.makeText(MainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
//							progressDialog.dismiss();
//						}
//					});
//				}
//			});
//			AlertDialog dialog = new AlertDialog.Builder(this)
//					.setTitle(android.R.string.dialog_alert_title)
//					.setMessage(R.string.missing_template)
//					.setPositiveButton(android.R.string.ok, null)
//					.show();
//			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//				@Override
//				public void onDismiss(DialogInterface dialog) {
//					progressDialog.show();
//				}
//			});
//		}
		try {
			db = readJson(openFileInput(DB_FILE_NAME));
			if (db == null) throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
			db = new JSONObject();
			try {
//				db.put(DB_KEY_SEARCH_ENGINE, R.string.default_se);
//				db.put(DB_KEY_SHOW_HIDDEN, false);
				db.put(DB_KEY_WIKI, new JSONArray());
//				db.put(DB_KEY_LAST_DIR, Environment.getExternalStorageDirectory().getAbsolutePath());
				writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			MainActivity.this.getWindow().setStatusBarColor(Color.WHITE);
			MainActivity.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			checkPermission();
		}
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		noWiki = findViewById(R.id.t_noWiki);
		try {
			if (db.getJSONArray(DB_KEY_WIKI).length() == 0)
				noWiki.setVisibility(View.VISIBLE);
			else
				noWiki.setVisibility(View.GONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rvWikiList = findViewById(R.id.rvWikiList);
		rvWikiList.setLayoutManager(new LinearLayoutManager(this));
		wikiListAdapter = new WikiListAdapter(this, db);
		wikiListAdapter.setReloadListener(new WikiListAdapter.ReloadListener() {
			@Override
			public void onReloaded(int count) {
				if (count > 0) noWiki.setVisibility(View.GONE);
				else noWiki.setVisibility(View.VISIBLE);
			}
		});
		wikiListAdapter.setOnItemClickListener(new WikiListAdapter.ItemClickListener() {
			@Override
			public void onItemClick(int position) {
				String id = wikiListAdapter.getId(position);
				Uri u = null;
				int i, m;
				try {
					m = db.getJSONArray(DB_KEY_WIKI).length();
					for (i = 0; i < m; i++) {
						JSONObject w = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i);
						if (w.getString(KEY_ID).equals(id)) {
							u = Uri.parse(w.getString(DB_KEY_URI));
							break;
						} else if (i == m - 1) throw new Exception();
					}
//					if (id != null && u != null && i < m) {
					//					File f = new File(vp);
					getContentResolver().takePersistableUriPermission(u, TAKE_FLAGS);
					if (new TWInfo(MainActivity.this, u).isWiki) {
						if (!loadPage(id))
							Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
					} else {
						final int p = i;
						final Uri r = u;
						new AlertDialog.Builder(MainActivity.this)
								.setTitle(android.R.string.dialog_alert_title)
								.setMessage(R.string.confirm_to_auto_remove_wiki)
								.setNegativeButton(android.R.string.no, null)
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										try {
											db.getJSONArray(DB_KEY_WIKI).remove(p);
											writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
												revokeUriPermission(getPackageName(), r, TAKE_FLAGS);
										} catch (Exception e) {
											e.printStackTrace();
										}
										MainActivity.this.onResume();
									}
								}).show();
					}
//					} else {
//						Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
//					}
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onItemLongClick(final int position) {
				try {
					final JSONObject w = db.getJSONArray(DB_KEY_WIKI).getJSONObject(position);
//					View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.wikiconfig_dialog, null);
//					final Button btnWikiConfigPath = view.findViewById(R.id.btnWikiConfigPath);
//					btnWikiConfigPath.setText(wikiData.getString(DB_KEY_PATH));
//					Button btnCreateShortcut = view.findViewById(R.id.btnCreateShortcut);
//					Button btnRemoveWiki = view.findViewById(R.id.btnRemoveWiki);

					Drawable icon = getResources().getDrawable(R.drawable.ic_description);
					FileInputStream is = null;
					Bitmap iconX = null;
					try {
						is = new FileInputStream(new File(getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), w.getString(KEY_ID)));
						iconX = BitmapFactory.decodeStream(is);
						if (iconX != null)
							icon = new BitmapDrawable(getResources(), iconX);
						else throw new Exception();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (is != null) try {
							is.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					final IconCompat iconCompat = iconX != null ? IconCompat.createWithBitmap(iconX) : IconCompat.createWithResource(MainActivity.this, R.drawable.ic_shortcut);
					final Uri u = Uri.parse(w.getString(DB_KEY_URI));
//					final View view = LayoutInflater.from(MainActivity.this).inflate(R.id.textView, null);
					final TextView view = new TextView(MainActivity.this);
					DocumentFile file = DocumentFile.fromSingleUri(MainActivity.this, u);
//					try {
//						System.out.println(u.getAuthority());
//						System.out.println(Uri.decode(u.getLastPathSegment()));
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					CharSequence d = u.getAuthority();
//
//					d = d + Uri.decode(u.getLastPathSegment());

					CharSequence s = getResources().getString(R.string.provider)
							+ u.getAuthority()
							+ '\n'
							+ getResources().getString(R.string.pathDir)
							+ Uri.decode(u.getLastPathSegment())
							+ '\n'
							+ getResources().getString(R.string.filename)
							+ (file != null ? file.getName() : getResources().getString(R.string.unknown));
					view.setText(s);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
						view.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
					final AlertDialog wikiConfigDialog = new AlertDialog.Builder(MainActivity.this)
							.setTitle(w.getString(MainActivity.KEY_NAME))
							.setIcon(icon)
//							.setView(view)
//							.setMessage(u.toString())
							.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									MainActivity.this.onResume();
								}
							})
							.setNegativeButton(R.string.remove_wiki, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									dialogInterface.dismiss();
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
													try {
														db.getJSONArray(DB_KEY_WIKI).remove(position);
														writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
														DocumentsContract.deleteDocument(getContentResolver(), u);
														if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
															revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
													} catch (Exception e) {
														e.printStackTrace();
													}
													MainActivity.this.onResume();
												}
											})
											.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													try {
														db.getJSONArray(DB_KEY_WIKI).remove(position);
														writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
														if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
															revokeUriPermission(getPackageName(), u, TAKE_FLAGS);
													} catch (Exception e) {
														e.printStackTrace();
													}
													MainActivity.this.onResume();
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
										String id = w.getString(KEY_ID);
										Bundle bu = new Bundle();
										bu.putString(KEY_ID, id);
										Intent in = new Intent(MainActivity.this, TWEditorWV.class).putExtras(bu).setAction(Intent.ACTION_MAIN);
										String lbl = w.getString(MainActivity.KEY_NAME);
										if (ShortcutManagerCompat.isRequestPinShortcutSupported(MainActivity.this)) {
											ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(MainActivity.this, id)
													.setShortLabel(lbl.substring(0, lbl.indexOf(KEY_LBL)))
													.setLongLabel(lbl)
													.setIcon(iconCompat)
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
//							.show();
							.create();
					int m = Math.round(20 * getResources().getDisplayMetrics().density + 0.5f);
					wikiConfigDialog.setView(view, m, 0, m, 0);
					wikiConfigDialog.show();
//					btnRemoveWiki.setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							View view1 = LayoutInflater.from(wikiConfigDialog.getContext()).inflate(R.layout.del_confirm, null);
//							final CheckBox cbDelFile = view1.findViewById(R.id.cbDelFile);
//							final CheckBox cbDelBackups = view1.findViewById(R.id.cbDelBackups);
//							cbDelBackups.setEnabled(false);
//							cbDelFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//								@Override
//								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//									cbDelBackups.setEnabled(isChecked);
//								}
//							});
//							AlertDialog removeWikiConfirmationDialog = new AlertDialog.Builder(wikiConfigDialog.getContext())
//									.setTitle(android.R.string.dialog_alert_title)
//									.setMessage(R.string.confirm_to_remove_wiki)
//									.setView(view1)
//									.setNegativeButton(android.R.string.cancel, null)
//									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											try {
//												final File f = new File(btnWikiConfigPath.getText().toString());
//												db.getJSONArray(DB_KEY_WIKI).remove(position);
//												writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
//												if (cbDelFile.isChecked()) {
//													try {
//														File[] fbx = f.getParentFile().listFiles(new FileFilter() {
//															@Override
//															public boolean accept(File pathname) {
//																return pathname.exists() && pathname.isDirectory() && pathname.getName().equals(f.getName() + BACKUP_DIRECTORY_PATH_PREFIX);
//															}
//														});
//														for (File fb : fbx)
//															if (cbDelBackups.isChecked() && fb.isDirectory()) {
//																File[] b = fb.listFiles(new FileFilter() {
//																	@Override
//																	public boolean accept(File pathname) {
//																		return isBackupFile(f, pathname);
//																	}
//																});
//																for (File f1 : b)
//																	f1.delete();
//																fb.delete();
//															}
//													} catch (Exception e) {
//														e.printStackTrace();
//													}
//													if (f.delete())
//														Toast.makeText(MainActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
//												}
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//											wikiConfigDialog.dismiss();
//											MainActivity.this.onResume();
//										}
//									})
//									.create();
//							removeWikiConfirmationDialog.show();
//						}
//					});
//					btnCreateShortcut.setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							try {
//								String id = wikiData.getString(KEY_ID);
//								Bundle bu = new Bundle();
//								bu.putString(KEY_ID, id);
//								Intent in = new Intent(MainActivity.this, TWEditorWV.class).putExtras(bu).setAction(Intent.ACTION_MAIN);
//								String lbl = wikiData.getString(MainActivity.KEY_NAME);
//								if (ShortcutManagerCompat.isRequestPinShortcutSupported(MainActivity.this)) {
//									ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(MainActivity.this, id)
//											.setShortLabel(lbl.substring(0, lbl.indexOf(KEY_LBL)))
//											.setLongLabel(lbl)
//											.setIcon(iconCompat)
//											.setIntent(in)
//											.build();
//									if (ShortcutManagerCompat.requestPinShortcut(MainActivity.this, shortcut, null))
//										Toast.makeText(MainActivity.this, R.string.shortcut_created, Toast.LENGTH_SHORT).show();
//									else throw new Exception();
//								}
//							} catch (Exception e) {
//								e.printStackTrace();
//								Toast.makeText(MainActivity.this, R.string.shortcut_failed, Toast.LENGTH_SHORT).show();
//							}
//						}
//					});
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	private Boolean loadPage(String id) {
		Intent in = new Intent();
		try {
			Bundle bu = new Bundle();
			String vid = null;
			for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
				if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID).equals(id)) {
					vid = id;
					break;
				}
			}
			if (vid != null) {
				bu.putString(KEY_ID, vid);
				in.putExtras(bu)
						.setClass(MainActivity.this, TWEditorWV.class);
				startActivity(in);
			} else throw new Exception();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

//	@Override
//	public boolean onMenuOpened(int featureId, Menu menu) {
//		if (menu != null) {
//			if (menu.getClass().getSimpleName().equalsIgnoreCase(CLASS_MENU_BUILDER)) {
//				try {
//					Method method = menu.getClass().getDeclaredMethod(METHOD_SET_OPTIONAL_ICONS_VISIBLE, Boolean.TYPE);
//					method.setAccessible(true);
//					method.invoke(menu, true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return super.onMenuOpened(featureId, menu);
//	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_new) {
			startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), 43);
//			final File template = new File(getFilesDir(), TEMPLATE_FILE_NAME);
//			if (template.exists() && new TWInfo(MainActivity.this, template).isWiki) {
//				File lastDir = Environment.getExternalStorageDirectory();
//				boolean showHidden = false;
//				try {
//					lastDir = new File(db.getString(DB_KEY_LAST_DIR));
//					showHidden = db.getBoolean(DB_KEY_SHOW_HIDDEN);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				FileDialog.fileSave(MainActivity.this, lastDir, HTML_FILTERS, showHidden, new FileDialog.OnFileTouchedListener() {
//					@Override
//					public void onFileTouched(File[] files) {
//						FileInputStream is = null;
//						FileOutputStream os = null;
//						try {
//							if (files != null && files.length > 0 && files[0] != null) {
//								File file = files[0];
//								is = new FileInputStream(template);
//								os = new FileOutputStream(file);
//								int len = is.available();
//								int length, lengthTotal = 0;
//								byte[] b = new byte[512];
//								while ((length = is.read(b)) != -1) {
//									os.write(b, 0, length);
//									lengthTotal += length;
//								}
//								os.flush();
//								if (lengthTotal != len) throw new Exception();
//								String id = genId();
//								try {
//									boolean exist = false;
//									for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//										if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_PATH).equals(file.getAbsolutePath())) {
//											exist = true;
//											id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
//											break;
//										}
//									}
//									if (exist) {
//										Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
//									} else {
//										TWInfo info = new TWInfo(MainActivity.this, file);
//										JSONObject w = new JSONObject();
//										w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getResources().getString(R.string.tiddlywiki));
//										w.put(KEY_ID, id);
//										w.put(DB_KEY_PATH, file.getAbsolutePath());
//										w.put(DB_KEY_BACKUP, false);
//										db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
//									}
//									db.put(DB_KEY_LAST_DIR, file.getParentFile().getAbsolutePath());
//									if (!MainActivity.writeJson(openFileOutput(DB_FILE_NAME, Context.MODE_PRIVATE), db))
//										throw new Exception();
//								} catch (Exception e) {
//									e.printStackTrace();
//									Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
//								}
//								MainActivity.this.onResume();
//								if (!loadPage(id))
//									Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//							} else throw new Exception();
//						} catch (Exception e) {
//							e.printStackTrace();
//							Toast.makeText(MainActivity.this, R.string.failed_creating_file, Toast.LENGTH_SHORT).show();
//						} finally {
//							if (is != null)
//								try {
//									is.close();
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							if (os != null)
//								try {
//									os.close();
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//						}
//					}
//
//					@Override
//					public void onCanceled() {
//
//					}
//				});
//			} else {
//				final ProgressDialog progressDialog = new ProgressDialog(this);
//				progressDialog.setMessage(getResources().getString(R.string.please_wait));
//				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//				progressDialog.setCancelable(false);
//				progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//					@Override
//					public void onShow(DialogInterface dialog) {
//						wGet(MainActivity.this, Uri.parse(getResources().getString(R.string.template_repo)), new File(getFilesDir(), TEMPLATE_FILE_NAME), true, true, new DownloadChecker() {
//							@Override
//							public boolean checkNg(File file) {
//								return !(new TWInfo(MainActivity.this, file).isWiki);
//							}
//						}, new OnDownloadCompleteListener() {
//							@Override
//							public void onDownloadComplete(File file) {
//								Toast.makeText(MainActivity.this, R.string.download_complete, Toast.LENGTH_SHORT).show();
//								progressDialog.dismiss();
//								onOptionsItemSelected(item);
//							}
//
//							@Override
//							public void onDownloadFailed() {
//								Toast.makeText(MainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
//								progressDialog.dismiss();
//							}
//						});
//					}
//				});
//				AlertDialog dialog = new AlertDialog.Builder(this)
//						.setTitle(android.R.string.dialog_alert_title)
//						.setMessage(R.string.missing_template)
//						.setPositiveButton(android.R.string.ok, null)
//						.show();
//				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//					@Override
//					public void onDismiss(DialogInterface dialog) {
//						progressDialog.show();
//					}
//				});
//			}
		} else if (id == R.id.action_import) {
			startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(TYPE_HTML), 42);
//			File lastDir = Environment.getExternalStorageDirectory();
//			boolean showHidden = false;
//			try {
//				lastDir = new File(db.getString(DB_KEY_LAST_DIR));
//				showHidden = db.getBoolean(DB_KEY_SHOW_HIDDEN);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			FileDialog.fileOpen(MainActivity.this, lastDir, HTML_FILTERS, showHidden, new FileDialog.OnFileTouchedListener() {
//				@Override
//				public void onFileTouched(File[] files) {
//					if (files != null && files.length > 0 && files[0] != null) {
//						File file = files[0];
//						String id = genId();
//						TWInfo info = new TWInfo(MainActivity.this, file);
//						if (info.isWiki) {
//							try {
//								boolean exist = false;
//								for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//									if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_PATH).equals(file.getAbsolutePath())) {
//										exist = true;
//										id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
//										break;
//									}
//								}
//								if (exist) {
//									Toast.makeText(MainActivity.this, R.string.wiki_already_exists, Toast.LENGTH_SHORT).show();
//								} else {
//									JSONObject w = new JSONObject();
//									w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getResources().getString(R.string.tiddlywiki));
//									w.put(KEY_ID, id);
//									w.put(DB_KEY_PATH, file.getAbsolutePath());
//									w.put(DB_KEY_BACKUP, false);
//									db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
//								}
//								db.put(DB_KEY_LAST_DIR, file.getParentFile().getAbsolutePath());
//								if (!MainActivity.writeJson(openFileOutput(DB_FILE_NAME, Context.MODE_PRIVATE), db))
//									throw new Exception();
//							} catch (Exception e) {
//								e.printStackTrace();
//								Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
//							}
//							MainActivity.this.onResume();
//							if (!loadPage(id))
//								Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//						} else {
//							Toast.makeText(MainActivity.this, R.string.not_a_wiki, Toast.LENGTH_SHORT).show();
//						}
//
//					} else
//						Toast.makeText(MainActivity.this, R.string.failed_opening_file, Toast.LENGTH_SHORT).show();
//				}
//
//				@Override
//				public void onCanceled() {
//
//				}
//			});
		} else if (id == R.id.action_about) {
			final SpannableString spannableString = new SpannableString(getResources().getString(R.string.about));
			Linkify.addLinks(spannableString, Linkify.ALL);
			final AlertDialog aboutDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.action_about)
					.setMessage(spannableString)
					.show();
			((TextView) aboutDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				((TextView) aboutDialog.findViewById(android.R.id.message)).setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		if (resultCode == Activity.RESULT_OK && resultData != null) {
			final Uri uri = resultData.getData();
			if (uri != null)
				switch (requestCode) {
					case 43:

						final ProgressDialog progressDialog = new ProgressDialog(this);
						progressDialog.setMessage(getResources().getString(R.string.please_wait));
						progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progressDialog.setCanceledOnTouchOutside(false);


//						final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//
//						Uri template = Uri.parse(getResources().getString(R.string.template_repo));
//						final DownloadManager.Request request = new DownloadManager.Request(template)
//								.setDestinationInExternalFilesDir(this, null, TEMPLATE_FILE_NAME)
//								.setVisibleInDownloadsUi(false)
//								.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
//						final Bundle bundle = new Bundle();
////						final long downloadId = downloadManager != null ? downloadManager.enqueue(request) : -1;
//						class CompleteReceiver extends BroadcastReceiver {
//							@Override
//							public void onReceive(Context context, Intent intent) {
//								long downloadId = bundle.getLong(KEY_DOWNLOAD);
//								if (downloadId != -1 && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId){
//									progressDialog.dismiss();
//									InputStream is = null;
//									OutputStream os = null;
//									try {
////									is = new FileInputStream(
////											downloadManager != null
////													? downloadManager.openDownloadedFile(downloadId).getFileDescriptor()
////													: new FileDescriptor());
////									DocumentFile.fromFile(new File(getExternalFilesDir(null),TEMPLATE_FILE_NAME));
////										String tempFn = genId();
////										File template = new File(getExternalFilesDir(null),tempFn);
//										File template = new File(getExternalFilesDir(null),genId());
//										is = new FileInputStream(template);
//										os = getContentResolver().openOutputStream(uri);
//										if (os != null) {
//										int len = is.available();
////										int length;
////										int lengthTotal = 0;
//										byte[] bytes = new byte[len];
////										while ((length = is.read(bytes)) > -1) {
////											os.write(bytes, 0, length);
////											lengthTotal += length;
////											if (Thread.currentThread().isInterrupted()) {
////												break;
////											}
////										}
//										os.write(bytes, 0, len);
//										os.flush();
//										//noinspection ResultOfMethodCallIgnored
//										template.renameTo(new File(getExternalFilesDir(null),TEMPLATE_FILE_NAME));
////										if (len > 0 && lengthTotal < len || !(new TWInfo(MainActivity.this, uri).isWiki))
//										if (!(new TWInfo(MainActivity.this, uri).isWiki))
//											throw new Exception();
//										progressDialog.dismiss();
//										String id = genId();
//										try {
//											boolean exist = false;
//											for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
//												if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_URI).equals(uri.toString())) {
//													exist = true;
//													id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
//													break;
//												}
//											}
//											if (exist) {
//												runOnUiThread(new Runnable() {
//													@Override
//													public void run() {
//														Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
//													}
//												});
//											} else {
//												TWInfo info = new TWInfo(MainActivity.this, uri);
//												JSONObject w = new JSONObject();
//
//												w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getResources().getString(R.string.tiddlywiki));
//												w.put(KEY_ID, id);
//												w.put(DB_KEY_URI, uri.toString());
//												db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
//											}
//											if (!MainActivity.writeJson(openFileOutput(DB_FILE_NAME, Context.MODE_PRIVATE), db))
//												throw new Exception();
//											getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
//										} catch (Exception e) {
//											e.printStackTrace();
//											runOnUiThread(new Runnable() {
//												@Override
//												public void run() {
//													Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
//												}
//											});
//										}
//										runOnUiThread(new Runnable() {
//											@Override
//											public void run() {
//												MainActivity.this.onResume();
//											}
//										});
//										if (!loadPage(id))
//											runOnUiThread(new Runnable() {
//												@Override
//												public void run() {
//													Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
//												}
//											});
//
//
//
//
//
//									}
//								}catch (Exception e){
//									e.printStackTrace();
//										try {
//											DocumentsContract.deleteDocument(getContentResolver(), uri);
//										} catch (Exception e1) {
//											e.printStackTrace();
//										}
//										runOnUiThread(new Runnable() {
//											@Override
//											public void run() {
//												Toast.makeText(MainActivity.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
//											}
//										});
//									} finally {
//										if (is != null)
//											try {
//												is.close();
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										if (os != null)
//											try {
//												os.close();
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//								}}
//							}
//						}


						final Thread thread = new Thread(new Runnable() {
							@Override
							public void run() {
								InputStream is = null;
								OutputStream os = null;
								boolean interrupted = false;
								try {
									os = getContentResolver().openOutputStream(uri);
									if (os != null) {
										URL url = new URL(getResources().getString(R.string.template_repo));
										HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
										httpsURLConnection.connect();
										int len = httpsURLConnection.getContentLength();
										is = httpsURLConnection.getInputStream();
										int length;
										int lengthTotal = 0;
										byte[] bytes = new byte[4096];
										while ((length = is.read(bytes)) > -1) {
											os.write(bytes, 0, length);
											lengthTotal += length;
											if (Thread.currentThread().isInterrupted()) {
												interrupted = true;
												break;
											}
										}
										os.flush();
										if (!interrupted) {
											if (len > 0 && lengthTotal < len || !(new TWInfo(MainActivity.this, uri).isWiki))
												throw new Exception();
											progressDialog.dismiss();
											String id = genId();
											try {
												boolean exist = false;
												for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
													if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_URI).equals(uri.toString())) {
														exist = true;
														id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
														break;
													}
												}
												if (exist) {
													runOnUiThread(new Runnable() {
														@Override
														public void run() {
															Toast.makeText(MainActivity.this, R.string.wiki_replaced, Toast.LENGTH_SHORT).show();
														}
													});
												} else {
													TWInfo info = new TWInfo(MainActivity.this, uri);
													JSONObject w = new JSONObject();

													w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getResources().getString(R.string.tiddlywiki));
													w.put(KEY_ID, id);
													w.put(DB_KEY_URI, uri.toString());
													db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
												}
												if (!MainActivity.writeJson(openFileOutput(DB_FILE_NAME, Context.MODE_PRIVATE), db))
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
											runOnUiThread(new Runnable() {
												@Override
												public void run() {
													MainActivity.this.onResume();
												}
											});
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
									try {
										DocumentsContract.deleteDocument(getContentResolver(), uri);
									} catch (Exception e1) {
										e.printStackTrace();
									}
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(MainActivity.this, R.string.error_processing_file, Toast.LENGTH_SHORT).show();
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
									if (interrupted) try {
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

						progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								progressDialog.cancel();
							}
						});
						progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
							@Override
							public void onShow(DialogInterface dialog) {
								thread.start();
//								bundle.putLong(KEY_DOWNLOAD,downloadManager != null ? downloadManager.enqueue(request) : -1);
//								IntentFilter intentFilter = new IntentFilter();
//								intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//								registerReceiver(new CompleteReceiver(), intentFilter);
							}
						});
						progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialogInterface) {
								thread.interrupt();
//								if (downloadManager!=null) downloadManager.remove(bundle.getLong(KEY_DOWNLOAD));
							}
						});
						progressDialog.show();
						break;
					case 42:
						String id = genId();
						TWInfo info = new TWInfo(MainActivity.this, uri);
						if (info.isWiki) {
							try {
								boolean exist = false;
								for (int i = 0; i < db.getJSONArray(DB_KEY_WIKI).length(); i++) {
									if (db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_URI).equals(uri.toString())) {
										exist = true;
										id = db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(KEY_ID);
										break;
									}
								}
								if (exist) {
									Toast.makeText(MainActivity.this, R.string.wiki_already_exists, Toast.LENGTH_SHORT).show();
								} else {
									JSONObject w = new JSONObject();
									w.put(KEY_NAME, (info.title != null && info.title.length() > 0) ? info.title : getResources().getString(R.string.tiddlywiki));
									w.put(KEY_ID, id);
									w.put(DB_KEY_URI, uri.toString());
//										w.put(DB_KEY_BACKUP, false);
									db.getJSONArray(DB_KEY_WIKI).put(db.getJSONArray(DB_KEY_WIKI).length(), w);
								}
//									db.put(DB_KEY_LAST_DIR, file.getParentFile().getAbsolutePath());
								if (!MainActivity.writeJson(openFileOutput(DB_FILE_NAME, Context.MODE_PRIVATE), db))
									throw new Exception();
								getContentResolver().takePersistableUriPermission(uri, TAKE_FLAGS);
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(MainActivity.this, R.string.data_error, Toast.LENGTH_SHORT).show();
							}
							MainActivity.this.onResume();
							if (!loadPage(id))
								Toast.makeText(MainActivity.this, R.string.error_loading_page, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(MainActivity.this, R.string.failed_opening_file, Toast.LENGTH_SHORT).show();
						}
						break;
				}
			// The document selected by the user won't be returned in the intent.
			// Instead, a URI to that document will be contained in the return intent
			// provided to this method as a parameter.
			// Pull that URI using resultData.getData().

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
			db = readJson(openFileInput(DB_FILE_NAME));
//			if (db != null) {
//				int i = 0;
//				if (db.getJSONArray(DB_KEY_WIKI).length() > 0)
//					do {
//						DocumentFile file = DocumentFile.fromSingleUri(this, Uri.parse(db.getJSONArray(DB_KEY_WIKI).getJSONObject(i).getString(DB_KEY_URI)));
//						if (!(file != null && file.exists() && file.isFile() && file.canRead() && file.canWrite()))
//							db.getJSONArray(DB_KEY_WIKI).remove(i);
//						else i++;
//					} while (i < db.getJSONArray(DB_KEY_WIKI).length());
//			}
//			writeJson(openFileOutput(DB_FILE_NAME, MODE_PRIVATE), db);
			wikiListAdapter.reload(db);
			rvWikiList.setAdapter(wikiListAdapter);
			if (db.getJSONArray(DB_KEY_WIKI).length() == 0)
				noWiki.setVisibility(View.VISIBLE);
			else
				noWiki.setVisibility(View.GONE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static JSONObject readJson(FileInputStream is) {
		byte[] b;
		JSONObject jsonObject = null;
		try {
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

	static boolean writeJson(FileOutputStream os, JSONObject vdb) {
		boolean v;
		try {
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

	@TargetApi(23)
	private void checkPermission() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		}
	}

	static String genId() {
		return UUID.randomUUID().toString();
	}

	static class TWInfo {
		boolean isWiki = false;
		String title = null;

		TWInfo(Context context, Uri uri) {
			try {
				InputStream is = context.getContentResolver().openInputStream(uri);
				Document doc = Jsoup.parse(is, null, uri.toString());
				Element ti = doc.getElementsByTag(KEY_TITLE).first();
				title = ti != null ? ti.html() : null;
				Element an = doc.getElementsByAttributeValue(KEY_NAME, KEY_APPLICATION_NAME).first();
				isWiki = an != null && an.attr(KEY_CONTENT).equals(context.getResources().getString(R.string.tiddlywiki));
				if (isWiki) return;
				Element ele = doc.getElementsByAttributeValue(KEY_ID, KEY_VERSION_AREA).first();
				StringBuilder stringBuilder = new StringBuilder(ele.html());
				int p = stringBuilder.indexOf(PREF_VER_3);
				stringBuilder.delete(p, p + PREF_VER_3.length());
				p = stringBuilder.indexOf(PREF_VER_4);
				stringBuilder.delete(p, p + PREF_VER_4.length());
				JSONObject jsonObject = new JSONObject(stringBuilder.substring(stringBuilder.indexOf(PREF_VER_1) + PREF_VER_1.length(), stringBuilder.indexOf(PREF_VER_2) + 1));
				try {
					isWiki = jsonObject.getString(KEY_TITLE).equals(context.getResources().getString(R.string.tiddlywiki));
				} catch (Exception e) {
					e.printStackTrace();
				}
//				if (js != null) {
//					org.mozilla.javascript.Context rhino = org.mozilla.javascript.Context.enter();
//					rhino.setOptimizationLevel(-1);
//					try {
//						Scriptable scope = rhino.initStandardObjects();
//						rhino.evaluateString(scope, js, context.getResources().getString(R.string.app_name), 1, null);
//						String c = (String) ((Scriptable) scope.get(KEY_VERSION, scope)).get(KEY_TITLE, scope);
//						isWiki = c != null && c.equals(context.getResources().getString(R.string.tiddlywiki));
//					} catch (Exception e) {
//						e.printStackTrace();
//					} finally {
//						org.mozilla.javascript.Context.exit();
//					}
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}