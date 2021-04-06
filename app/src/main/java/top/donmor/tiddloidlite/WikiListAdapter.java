/*
 * top.donmor.tiddloidlite.WikiListAdapter <= [P|Tiddloid Lite]
 * Last modified: 05:03:14 2019/05/07
 * Copyright (c) 2019 donmor
 */

package top.donmor.tiddloidlite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class WikiListAdapter extends RecyclerView.Adapter<WikiListAdapter.WikiListHolder> {

	private final Context context;
	private JSONObject wl;
//	private int count;
	private ArrayList<String> ids;
	private ItemClickListener mItemClickListener;
	private ReloadListener mReloadListener;
	private final LayoutInflater inflater;
	private final Vibrator vibrator;
	private final float scale;
	private static final String c160 = "\u00A0", zeroB = "0\u00A0B", PAT_SIZE = "\u00A0\u00A0\u00A0\u00A0#,##0.##";
	private static final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};

	WikiListAdapter(Context context, JSONObject db) throws JSONException {
		this.context = context;
		scale = context.getResources().getDisplayMetrics().density;
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
//		try {
		this.wl = db.getJSONObject(MainActivity.DB_KEY_WIKI);
		Iterator<String> iterator = wl.keys();
//		int i = 0;
		ids = new ArrayList<>();
		while (iterator.hasNext()) {
			ids.add(iterator.next());
//			ids[i] = iterator.next();
//			i++;
		}
//		count = wl.length();
//			count = db.getJSONArray(MainActivity.DB_KEY_WIKI).length();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		inflater = LayoutInflater.from(context);
	}

	static class WikiListHolder extends RecyclerView.ViewHolder {
		private final Button btnWiki;
		private Uri uri;
		private String id;

		WikiListHolder(View itemView) {
			super(itemView);
			btnWiki = itemView.findViewById(R.id.btnWiki);
			btnWiki.setVisibility(View.GONE);
		}
	}

	@Override
	@NonNull
	public WikiListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new WikiListHolder(inflater.inflate(R.layout.wiki_slot, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final WikiListHolder holder, int position) {
		try {
			holder.id = ids.get(position);
			JSONObject w = wl.getJSONObject(holder.id);
//			JSONObject w = db.getJSONArray(MainActivity.DB_KEY_WIKI).getJSONObject(position);
			holder.uri = Uri.parse(w.getString(MainActivity.DB_KEY_URI));
			//			try {
			//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			String n = w.getString(MainActivity.KEY_NAME), s = w.optString(MainActivity.DB_KEY_SUBTITLE), fib64 = w.optString(MainActivity.KEY_FAVICON);
			if (fib64.length() > 0) {
				byte[] b = Base64.decode(fib64, Base64.NO_PADDING);
				Bitmap favicon = BitmapFactory.decodeByteArray(b, 0, b.length);
				if (favicon != null) {
					int width = favicon.getWidth(), height = favicon.getHeight();
					Matrix matrix = new Matrix();
					matrix.postScale(scale * 24f / width, scale * 24f / height);
					holder.btnWiki.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), Bitmap.createBitmap(favicon, 0, 0, width, height, matrix, true)), null, null, null);
				}
			}

//			FileInputStream is = null;
//			Bitmap icon = null;
//			try {
//				File iconFile = new File(context.getDir(MainActivity.KEY_FAVICON, Context.MODE_PRIVATE), id);
//				if (iconFile.exists() && iconFile.length() > 0) {
//					is = new FileInputStream(iconFile);
//					icon = BitmapFactory.decodeStream(is);
//					if (icon != null) {
//						int width = icon.getWidth(), height = icon.getHeight();
//						Matrix matrix = new Matrix();
//						matrix.postScale(scale * 24f / width, scale * 24f / height);
//						Bitmap favicon = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
//						holder.btnWiki.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), favicon), null, null, null);
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (is != null) try {
//					is.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			final Bitmap fi = icon;
			holder.btnWiki.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mItemClickListener.onItemClick(holder.id);
//					mItemClickListener.onItemClick(position, id, holder.uri);
				}
			});
			holder.btnWiki.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					vibrator.vibrate(new long[]{0, 1}, -1);
					mItemClickListener.onItemLongClick(holder.id);
//					mItemClickListener.onItemLongClick(position, id, holder.uri, n, s, fi);
					return true;
				}
			});
			try {
				SpannableStringBuilder builder = new SpannableStringBuilder(n);
				int p = builder.length();
				LeadingMarginSpan.Standard lms = new LeadingMarginSpan.Standard(Math.round(scale * 8f));
				builder.setSpan(lms, 0, p, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
				builder.append(s != null && s.length() > 0 ? MainActivity.KEY_LBL + s : MainActivity.STR_EMPTY);
				builder.append('\n');
				ForegroundColorSpan fcs = new ForegroundColorSpan(context.getResources().getColor(R.color.content_sub));
				builder.setSpan(fcs, p, builder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
				p = builder.length();
				DocumentFile documentFile = DocumentFile.fromSingleUri(context, holder.uri);
				if (documentFile != null && documentFile.exists())
					builder.append(SimpleDateFormat.getDateTimeInstance().format(new Date(documentFile.lastModified()))).append(formatSize(documentFile.length()));
				else builder.append(MainActivity.STR_EMPTY);
				RelativeSizeSpan rss = new RelativeSizeSpan(0.8f);
				builder.setSpan(rss, p, builder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
				holder.btnWiki.setText(builder);
			} catch (Exception e) {
				e.printStackTrace();
			}
			holder.btnWiki.setVisibility(View.VISIBLE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getItemCount() {
		return ids.size();
	}

	interface ItemClickListener {
		void onItemClick(String id);
//		void onItemClick(int position, String id, Uri uri);

		void onItemLongClick(String id);
//		void onItemLongClick(int position, String id, Uri uri, String name, String sub, Bitmap favicon);
	}

	void setOnItemClickListener(ItemClickListener itemClickListener) {
		this.mItemClickListener = itemClickListener;
	}

	interface ReloadListener {
		void onReloaded(int count);
	}

	void setReloadListener(ReloadListener reloadListener) {
		this.mReloadListener = reloadListener;
	}

	void reload(JSONObject db) throws JSONException {
		this.wl = db.getJSONObject(MainActivity.DB_KEY_WIKI);
		Iterator<String> iterator = wl.keys();
		ids = new ArrayList<>();
		while (iterator.hasNext()) {
			ids.add(iterator.next());
		}
//		count = wl.length();
		mReloadListener.onReloaded(this.getItemCount());
	}

	private String formatSize(long size) {
		if (size <= 0)
			return zeroB;
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat(PAT_SIZE).format(size / Math.pow(1024, digitGroups)) + c160 + units[digitGroups];
	}
}
