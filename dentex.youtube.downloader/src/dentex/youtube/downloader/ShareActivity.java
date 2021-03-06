/***
 	Copyright (c) 2012-2013 Samuele Rini
 	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program. If not, see http://www.gnu.org/licenses
	
	***
	
	https://github.com/dentex/ytdownloader/
    https://sourceforge.net/projects/ytdownloader/
	
	***
	
	Different Licenses and Credits where noted in code comments.
*/

package dentex.youtube.downloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.matsuhiro.android.connect.NetworkUtils;
import com.matsuhiro.android.download.DownloadTask;
import com.matsuhiro.android.download.DownloadTaskListener;
import com.matsuhiro.android.download.Maps;

import dentex.youtube.downloader.menu.AboutActivity;
import dentex.youtube.downloader.menu.DonateActivity;
import dentex.youtube.downloader.menu.TutorialsActivity;
import dentex.youtube.downloader.queue.FFmpegExtractAudioTask;
import dentex.youtube.downloader.utils.FetchUrl;
import dentex.youtube.downloader.utils.Json;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.RhinoRunner;
import dentex.youtube.downloader.utils.Utils;

public class ShareActivity extends Activity {
	
	private ProgressBar progressBar1;
	private ProgressBar progressBarD;
	private ProgressBar progressBarL;
	private static final String DEBUG_TAG = "ShareActivity";
    private TextView tv;
    private TextView noVideoInfo;
    private ListView lv;
    private ArrayAdapter<String> aA;
    private List<String> links = new ArrayList<String>();
    private List<String> codecs = new ArrayList<String>();
    private List<String> qualities = new ArrayList<String>();
    private List<String> sizes = new ArrayList<String>();
    private List<String> itags = new ArrayList<String>();
    private List<String> listEntries = new ArrayList<String>();
    //private int index = 0;
    private int ueIndex = 0;
    private int asIndex = 0;
    private String titleRaw;
    private String basename;
    private int pos;
    private File path;
    private String validatedLink;
    private String vFilename = "";
    public static Uri videoUri;
    private int icon;
    private CheckBox showAgain1;
    private CheckBox showAgain2;
    private TextView userFilename;
    private boolean sshInfoCheckboxEnabled;
    private boolean generalInfoCheckboxEnabled;
    private boolean fileRenameEnabled;
    private File chooserFolder;
	private AsyncDownload asyncDownload;
	private AsyncSizesFiller asyncSizesFiller;
	private boolean isAsyncDownloadRunning = false;
	private boolean isAsyncSizesFillerRunning = false;
	private AlertDialog helpDialog;
	private AlertDialog.Builder  helpBuilder;
	private Bitmap img;
	private ImageView imgView;
	private String videoId;
	public static Context sShare;
	private ContextThemeWrapper boxThemeContextWrapper = new ContextThemeWrapper(this, R.style.BoxTheme);
	private String[] decryptionArray = null;
	private String jslink;
	private String decryptionRule = null;
	private String decryptionFunction;
	private DownloadTaskListener dtl;
	private boolean autoModeEnabled = false;
	private boolean restartModeEnabled = false;
	private String extraId;
	private boolean autoFFmpegTaskAlreadySent = false;
	public String mComposedName;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BugSenseHandler.leaveBreadcrumb("ShareActivity_onCreate");
        sShare = getBaseContext();
        
    	// Theme init
    	Utils.themeInit(this);
    	
        setContentView(R.layout.activity_share);
        
    	//showSizesInVideoList = YTD.settings.getBoolean("show_size_list", false);

    	// Language init
    	Utils.langInit(this);
        
        // loading views from the layout xml
        tv = (TextView) findViewById(R.id.textView1);
        noVideoInfo = (TextView) findViewById(R.id.share_activity_info);
        
        progressBarD = (ProgressBar) findViewById(R.id.progressBarD);
        progressBarL = (ProgressBar) findViewById(R.id.progressBarL);
        
        String theme = YTD.settings.getString("choose_theme", "D");
    	if (theme.equals("D")) {
    		progressBar1 = progressBarD;
    		progressBarL.setVisibility(View.GONE);
    	} else {
    		progressBar1 = progressBarL;
    		progressBarD.setVisibility(View.GONE);
    	}

        imgView = (ImageView)findViewById(R.id.imgview);
        
        lv = (ListView) findViewById(R.id.list);

        // YTD update initialization
        updateInit();
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
        	BugSenseHandler.leaveBreadcrumb("Intent.ACTION_SEND");
            if ("text/plain".equals(type)) {
                try {
                	handleSendText(intent, action);
                	Utils.logger("d", "handling ACTION_SEND", DEBUG_TAG);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Error: " + e.getMessage(), e);
                }
            }
        }
        
        if (Intent.ACTION_VIEW.equals(action)) {
        	BugSenseHandler.leaveBreadcrumb("Intent.ACTION_VIEW");
        	if (intent.hasCategory("AUTO")) {
        		autoModeEnabled = true;
        		extraId = intent.getStringExtra("id");
        		pos = intent.getIntExtra("position", 0);
        		vFilename = intent.getStringExtra("filename");
        		
        		Utils.logger("i", "Auto Mode Enabled:"
        				+ "\n -> id: " + extraId
        				+ "\n -> position: " + pos
        				+ "\n -> filename: " + vFilename, DEBUG_TAG);
        	} else if (intent.hasCategory("RESTART")) {
        		restartModeEnabled = true; 
        		extraId = intent.getStringExtra("id");
        		
        		Utils.logger("i", "Restart Mode Enabled:"
        				+ "\n -> id: " + extraId, DEBUG_TAG);
        	}
            try {
            	handleSendText(intent, action);
            	Utils.logger("d", "handling ACTION_VIEW", DEBUG_TAG);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error: " + e.getMessage(), e);
            }
        }
    }

    public static Context getContext() {
        return sShare;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (!autoModeEnabled) {
	        switch(item.getItemId()){
	        	case R.id.menu_donate:
	    			startActivity(new Intent(this, DonateActivity.class));
	    			return true;
	        	case R.id.menu_settings:
	        		startActivity(new Intent(this, SettingsActivity.class));
	        		return true;
	        	case R.id.menu_about:
	        		startActivity(new Intent(this, AboutActivity.class));
	        		return true;
	        	case R.id.menu_dashboard:
				launchDashboardActivity();
	        		return true;
	        	case R.id.menu_tutorials:
	        		startActivity(new Intent(this, TutorialsActivity.class));
	        		return true;
	        	default:
	        		return super.onOptionsItemSelected(item);
	        }
        } else {
        	return super.onOptionsItemSelected(item);
        }
    }

	private void launchDashboardActivity() {
		Intent dashboardIntent = new Intent(this, DashboardActivity.class);
		dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(dashboardIntent);
	}
    
    /*@Override
    protected void onStart() {
        super.onStart();
        Utils.logger("v", "_onStart", DEBUG_TAG);
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	Utils.logger("v", "_onRestart");
    }

    @Override
    public void onPause() {
    	super.onPause();
    	Utils.logger("v", "_onPause");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    	Utils.logger("v", "_onStop", DEBUG_TAG);
    }*/
    
    @Override
	public void onBackPressed() {
    	Utils.logger("v", "_onBackPressed", DEBUG_TAG);
    	super.onBackPressed();
    	
    	// To cancel the AsyncDownload AsyncSizesFiller tasks only on back button pressed (not when switching to other activities)
    	if (isAsyncDownloadRunning) {
    		Utils.logger("v", "canceling asyncDownload", DEBUG_TAG);
    		asyncDownload.cancel(true);
    	}
    	if (isAsyncSizesFillerRunning) {
    		Utils.logger("v", "canceling asyncSizesFiller", DEBUG_TAG);
    		asyncSizesFiller.cancel(true);
    	}
	}

    void handleSendText(Intent intent, String action) throws IOException {

        /*ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {*/
        if (NetworkUtils.isNetworkAvailable(sShare)) {
        	String sharedText = null;
			if (action.equals(Intent.ACTION_SEND)) {
            	sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        	} else if (action.equals(Intent.ACTION_VIEW)) {
        		sharedText = intent.getDataString();
        	}
            
			if (sharedText != null) {
	            if (linkValidator(sharedText) == "bad_link") {
	            	badOrNullLinkAlert();
	            } else if (sharedText != null) {
	            	showGeneralInfoTutorial();
	            	asyncDownload = new AsyncDownload();
	            	asyncDownload.execute(validatedLink);
	            }
			} else {
				badOrNullLinkAlert();
			}
        } else {
        	progressBar1.setVisibility(View.GONE);
        	tv.setVisibility(View.GONE);
        	noVideoInfo.setText(getString(R.string.no_net));
        	noVideoInfo.setVisibility(View.VISIBLE);
        	PopUps.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
        	Button retry = (Button) findViewById(R.id.share_activity_retry_button);
        	retry.setVisibility(View.VISIBLE);
        	retry.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Utils.reload(ShareActivity.this);					
				}
			});
        }
    }

	public void badOrNullLinkAlert() {
		BugSenseHandler.leaveBreadcrumb("badOrNullLinkAlert");
		progressBar1.setVisibility(View.GONE);
		PopUps.showPopUp(getString(R.string.error), getString(R.string.bad_link_dialog_msg), "alert", this);
		tv.setVisibility(View.GONE);
		noVideoInfo.setText(getString(R.string.bad_link));
		noVideoInfo.setVisibility(View.VISIBLE);
	}
    
    private void showGeneralInfoTutorial() {
        generalInfoCheckboxEnabled = YTD.settings.getBoolean("general_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_general_info, null);
    	    showAgain1 = (CheckBox) generalInfo.findViewById(R.id.showAgain1);
    	    showAgain1.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.tutorial_title));    	    
    	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		if (!showAgain1.isChecked()) {
    	    			YTD.settings.edit().putBoolean("general_info", false).commit();
    	    			sshInfoCheckboxEnabled = YTD.settings.getBoolean("general_info", true);
    	    			Utils.logger("v", "generalInfoCheckboxEnabled: " + generalInfoCheckboxEnabled, DEBUG_TAG);
    	    		}
        		}
        	});
    	    if (! ((Activity) this).isFinishing()) {
    	    	adb.show();
    	    }
        }
    }
    
    private String linkValidator(String sharedText) {
    	Pattern pattern = Pattern.compile("(http://|https://).*(v=.{11}).*");
        Matcher matcher = pattern.matcher(sharedText);
        if (matcher.find()) {
            //validatedLink = matcher.group(1) + "www.youtube.com/watch?" + matcher.group(2);
            validatedLink = "http://www.youtube.com/watch?" + matcher.group(2);
            videoId = matcher.group(2).replace("v=", "");
            return validatedLink;
        }
        return "bad_link";
    }
    
    public void assignPath() {
    	boolean Location = YTD.settings.getBoolean("swap_location", false);
        
        if (Location == false) {
            String location = YTD.settings.getString("standard_location", "Downloads");
            Utils.logger("d", "location: " + location, DEBUG_TAG);
            
            if (location.equals("DCIM") == true) {
            	path = YTD.dir_DCIM;
            }
            if (location.equals("Movies") == true) {
            	path = YTD.dir_Movies;
            } 
            if (location.equals("Downloads") == true) {
            	path = YTD.dir_Downloads;
            }
            
        } else {
        	String cs = YTD.settings.getString("CHOOSER_FOLDER", "");
        	chooserFolder = new File(cs);
        	if (chooserFolder.exists()) {
        		Utils.logger("d", "chooserFolder: " + chooserFolder, DEBUG_TAG);
        		path = chooserFolder;
        	} else {
        		path = YTD.dir_Downloads;
        		Utils.logger("w", "chooserFolder not found, falling back to Download path", DEBUG_TAG);
        	}
        }
        
        if (!path.exists()) {
        	if (new File(path.getAbsolutePath()).mkdirs()) {
        		Utils.logger("w", "destination path not found, creating it now", DEBUG_TAG);
        	} else {
        		Log.e(DEBUG_TAG, "Something really bad happened with the download destination...");
        	}
        	
        }
        	
        Utils.logger("d", "path: " + path, DEBUG_TAG);
    }

    private class AsyncDownload extends AsyncTask<String, Integer, String> {

		protected void onPreExecute() {
    		isAsyncDownloadRunning = true;
    		tv.setText(R.string.loading);
    		progressBar1.setIndeterminate(true);
    		progressBar1.setVisibility(View.VISIBLE);
    	}
    	
    	protected String doInBackground(String... urls) {
            try {
            	Utils.logger("d", "doInBackground...", DEBUG_TAG);
            	assignBitmapToVideoListThumbnail(generateThumbUrls());

            	FetchUrl fu = new FetchUrl(sShare);
            	return urlBlockMatchAndDecode(fu.doFetch(urls[0])); 
            } catch (Exception e) {
            	Log.e(DEBUG_TAG, "downloadUrl: " + e.getMessage());
		    	BugSenseHandler.sendExceptionMessage(DEBUG_TAG + "-> downloadUrl: ", e.getMessage(), e);
                return "e";
            } 
        }
    	
    	public void doProgress(int value){
            publishProgress(value);
        }
    	
    	protected void onProgressUpdate(Integer... values) {
    		progressBar1.setProgress(values[0]);
    	}

        @Override
        protected void onPostExecute(String result) {

        	progressBar1.setVisibility(View.GONE);
        	
        	isAsyncDownloadRunning = false;
        	
        	if (YTD.settings.getBoolean("show_thumb", false) && 
        			!((result == null || result.equals("e")) ||
        			  (result != null && result.equals("login_required")) ||
        			  (result != null && result.equals("rtmpe")) ) ) {
        		imgView.setImageBitmap(img);
        	}
        	
            if (result == null || result.equals("e") && !autoModeEnabled) {
            	BugSenseHandler.leaveBreadcrumb("invalid_url");
            	noVideosMsgs("alert", getString(R.string.invalid_url));
            }
            
            if (result != null && result.equals("login_required") && !autoModeEnabled) {
            	BugSenseHandler.leaveBreadcrumb("login_required");
            	noVideosMsgs("info", getString(R.string.login_required));
            }
            
            if (result != null && result.equals("rtmpe")) {
            	BugSenseHandler.leaveBreadcrumb("encrypted_streams");
            	listEntries.clear();
            	noVideosMsgs("info", getString(R.string.encrypted_streams));
            }
            
            aA = new ShareListAdapter(listEntries, ShareActivity.this);
            
            if (autoModeEnabled) {
            	BugSenseHandler.leaveBreadcrumb("autoModeEnabled");
            	assignPath();
            	
            	try {
            		callDownloadManager(links.get(pos), pos, vFilename);
            	} catch (IndexOutOfBoundsException e) {
            		Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
            		launchDashboardActivity();
            	}
            } else {
            	lv.setAdapter(aA);
            	
            	asyncSizesFiller = new AsyncSizesFiller();
            	asyncSizesFiller.execute(links.toArray(new String[0]));
            }

            tv.setText(titleRaw);
            
            lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Utils.logger("i", "Selected link: " + links.get(pos), DEBUG_TAG);
					BugSenseHandler.leaveBreadcrumb("ShareActivity_onItemClick");
					assignPath();
					
                    pos = position;     
                    //pos = 45;		// to test IndexOutOfBound Exception...
                    
                    final String base = composeVideoFilenameNoExt();
                    vFilename = composeVideoFilename(base);
                    
                	helpBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
                    helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                    helpBuilder.setTitle(getString(R.string.list_click_dialog_title));

					boolean showSize = false;
					try {
                        if (sizes.get(pos).equals("")) {
                        	helpBuilder.setMessage(titleRaw + "\n" +
	                        		getString(R.string.quality) + " " + itags.get(pos));
                        } else {
                        	helpBuilder.setMessage(titleRaw +  "\n" +
        							getString(R.string.quality) + " " + itags.get(pos) +
        							getString(R.string.size) + " " + sizes.get(pos).replace(" - ", ""));
                        }
                        
					} catch (IndexOutOfBoundsException e) {
			    		Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
			    	}
					
                    helpBuilder.setPositiveButton(getString(R.string.list_click_download_local), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        	try {
                        		fileRenameEnabled = YTD.settings.getBoolean("enable_rename", false);
	                            if (fileRenameEnabled == true) {
									AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
	                            	LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
		                    	    View inputFilename = adbInflater.inflate(R.layout.dialog_input_filename, null);
		                    	    userFilename = (TextView) inputFilename.findViewById(R.id.input_filename);
		                    	    userFilename.setText(base);
		                    	    adb.setView(inputFilename);
		                    	    adb.setTitle(getString(R.string.rename_dialog_title));
		                    	    adb.setMessage(getString(R.string.rename_dialog_msg));
		                    	    
		                    	    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		                    	    	public void onClick(DialogInterface dialog, int which) {
		                    	    		mComposedName = userFilename.getText().toString();
											vFilename = composeVideoFilename(mComposedName);
											callDownloadManager(links.get(pos), pos, vFilename);
		                    	    	}
		                    	    });
		                    	    
		                    	    adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
		                	        	public void onClick(DialogInterface dialog, int which) {
		                	                // cancel
		                	            }
		                	        });
		                    	    
		                    	    if (! ((Activity) ShareActivity.this).isFinishing()) {
		                    	    	adb.show();
		                    	    }
	                            } else {
	                            	callDownloadManager(links.get(pos), pos, vFilename);
	                            }
                        	} catch (IndexOutOfBoundsException e) {
    							Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
    						}
                        }
                    });
					
                    // show central button for SSH send if enabled in prefs
                    if (!YTD.settings.getBoolean("ssh_to_longpress_menu", false)) {
	                    helpBuilder.setNeutralButton(getString(R.string.list_click_download_ssh), new DialogInterface.OnClickListener() {
	
	                        public void onClick(DialogInterface dialog, int which) {
	                        	sendViaSsh();
	                        }
	                    });
                    }

                    helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(ShareActivity.this, "Download canceled...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    if (!showSize) {
                    	helpDialog = helpBuilder.create();
                    	
                    	if (! ((Activity) ShareActivity.this).isFinishing()) {
                    		helpDialog.show();
                	    }
                    }
                }
            });
            
            lv.setLongClickable(true);
            lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            	@Override
            	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            		BugSenseHandler.leaveBreadcrumb("ShareActivity_onItemLongClick");
            		pos = position;
            		
            		String base = composeVideoFilenameNoExt();
            		vFilename = composeVideoFilename(base);
            		
            		AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
            		if (!YTD.settings.getBoolean("ssh_to_longpress_menu", false)) {
	            		builder.setTitle(R.string.long_click_title).setItems(R.array.long_click_entries, new DialogInterface.OnClickListener() {
					    	public void onClick(DialogInterface dialog, int which) {
					    		switch (which) {
					    			case 0: // copy
					    				copy(position);
					    				break;
					    			case 1: // share
					    				share(position, vFilename);
					    		}
					    	}
	            		});
            		} else {
            			builder.setTitle(R.string.long_click_title).setItems(R.array.long_click_entries2, new DialogInterface.OnClickListener() {
					    	public void onClick(DialogInterface dialog, int which) {
					    		switch (which) {
					    			case 0: // copy
					    				copy(position);
					    				break;
					    			case 1: // share
					    				share(position, vFilename);
					    				break;
					    			case 2: // SSH
					    				sendViaSsh();
					    		}
					    	}
	            		});
            		}
            		builder.create();
            		if (! ((Activity) ShareActivity.this).isFinishing()) {
            			builder.show();
            		}
				    return true;
            	}
            });
        }

		private void noVideosMsgs(String type, String cause) {
			PopUps.showPopUp(getString(R.string.no_video_available), cause, type, ShareActivity.this);
			tv.setVisibility(View.GONE);
			noVideoInfo.setVisibility(View.VISIBLE);
		}
        
        private void share(final int position, String filename) {
        	BugSenseHandler.leaveBreadcrumb("ShareActivity_share");
			Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, filename);
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, links.get(position));
			startActivity(Intent.createChooser(sharingIntent, "Share YouTube link:"));
		}

		private void copy(final int position) {
			BugSenseHandler.leaveBreadcrumb("ShareActivity_copy");
			ClipData cmd = ClipData.newPlainText("simple text", links.get(position));
			ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cb.setPrimaryClip(cmd);
		}
        
		private String composeVideoFilenameNoExt() {
        	String suffix = itags.get(pos)
        			.replace("MP4 - ", "")
        			.replace("WebM - ", "")
        			.replace("FLV - ", "")
        			.replace("3GP - ", "")
        			.replace("/", "-")
        			.replace(" - ", "_");
        	
        	String composedName = basename + "_" + suffix;
        	
    	    Utils.logger("d", "videoFilename with no EXT: " + composedName, DEBUG_TAG);
    	    mComposedName = composedName;
    	    return composedName;
        }
		
		private String composeVideoFilename(String base) {
        	
        	String composedName = base + "." + codecs.get(pos);
        	
    	    Utils.logger("d", "COMPLETE videoFilename: " + composedName, DEBUG_TAG);
    	    return composedName;
        }

		private void callConnectBot() {
			BugSenseHandler.leaveBreadcrumb("callConnectBot");
        	Context context = getApplicationContext();
    		PackageManager pm = context.getPackageManager();
    		
    		final String connectBotFlavour = YTD.settings.getString("connectbot_flavour", "org.connectbot");
    		
    		String connectBotFlavourPlain = "ConnectBot";
    		if (connectBotFlavour.equals("sk.vx.connectbot")) connectBotFlavourPlain = "VX " + connectBotFlavourPlain;
    		if (connectBotFlavour.equals("org.woltage.irssiconnectbot")) connectBotFlavourPlain = "Irssi " + connectBotFlavourPlain;
    		
			Intent appStartIntent = pm.getLaunchIntentForPackage(connectBotFlavour);
    		if (null != appStartIntent) {
    			Utils.logger("d", "appStartIntent: " + appStartIntent, DEBUG_TAG);
    			context.startActivity(appStartIntent);
    		} else {
    			AlertDialog.Builder cb = new AlertDialog.Builder(boxThemeContextWrapper);
    	        cb.setTitle(getString(R.string.callConnectBot_dialog_title, connectBotFlavourPlain));
    	        cb.setMessage(getString(R.string.callConnectBot_dialog_msg));
    	        icon = android.R.drawable.ic_dialog_alert;
    	        cb.setIcon(icon);
    	        cb.setPositiveButton(getString(R.string.callConnectBot_dialog_positive), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int which) {
    	            	Intent intent = new Intent(Intent.ACTION_VIEW); 
    	            	intent.setData(Uri.parse("market://details?id=" + connectBotFlavour));
    	            	try {
    	            		startActivity(intent);
    	            	} catch (ActivityNotFoundException exception){
    	            		PopUps.showPopUp(getString(R.string.no_market), getString(R.string.no_net_dialog_msg), "alert", ShareActivity.this);
    	            	}
    	            }
    	        });
    	        cb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
    	        	public void onClick(DialogInterface dialog, int which) {
    	                // cancel
    	            }
    	        });

    	        AlertDialog helpDialog = cb.create();
    	        
    	        if (! ((Activity) ShareActivity.this).isFinishing()) {
    	        	helpDialog.show();
        		}
    		}
        }

		private void sendViaSsh() {
			BugSenseHandler.leaveBreadcrumb("sendViaSsh");
			try {
				String wgetCmd;
				
				Boolean shortSshCmdEnabled = YTD.settings.getBoolean("enable_connectbot_short_cmd", false);
				if (shortSshCmdEnabled) {
					wgetCmd = "wget -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
							links.get(pos) + "\' -O " + vFilename;
				} else {
					wgetCmd = "REQ=`wget -q -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
							validatedLink + "\' -O-` && urlblock=`echo $REQ | grep -oE \'url_encoded_fmt_stream_map\": \".*\' | sed -e \'s/\", \".*//\'" + 
							" -e \'s/url_encoded_fmt_stream_map\": \"//\'` && urlarray=( `echo $urlblock | sed \'s/,/\\n\\n/g\'` ) && N=" + pos + 
							" && block=`echo \"${urlarray[$N]}\" | sed -e \'s/%3A/:/g\' -e \'s/%2F/\\//g\' -e \'s/%3F/\\?/g\' -e \'s/%3D/\\=/g\'" + 
							" -e \'s/%252C/%2C/g\' -e \'s/%26/\\&/g\' -e \'s/%253A/\\:/g\' -e \'s/\", \"/\"-\"/\' -e \'s/sig=/signature=/\'" + 
							" -e \'s/x-flv/flv/\' -e \'s/\\\\\\u0026/\\&/g\'` && url=`echo $block | grep -oE \'http://.*\' | sed -e \'s/&type=.*//\'" + 
							" -e \'s/&signature=.*//\' -e \'s/&quality=.*//\' -e \'s/&fallback_host=.*//\'` && sig=`echo $block | " +
							"grep -oE \'signature=.{81}\'` && downloadurl=`echo $url\\&$sig | sed \'s/&itag=[0-9][0-9]&signature/\\&signature/\'` && " +
							"wget -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --tries=5 --timeout=45 --no-check-certificate " +
							"\"$downloadurl\" -O " + vFilename;
				}
				
				Utils.logger("d", "wgetCmd: " + wgetCmd, DEBUG_TAG);
			    
				ClipData cmd = ClipData.newPlainText("simple text", wgetCmd);
			    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    cb.setPrimaryClip(cmd);
			    
			    sshInfoCheckboxEnabled = YTD.settings.getBoolean("ssh_info", true);
			    if (sshInfoCheckboxEnabled == true) {
			        AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
				    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
				    View showAgain = adbInflater.inflate(R.layout.dialog_show_again_checkbox, null);
				    showAgain2 = (CheckBox) showAgain.findViewById(R.id.showAgain2);
				    showAgain2.setChecked(true);
				    adb.setView(showAgain);
				    adb.setTitle(getString(R.string.ssh_info_tutorial_title));
				    adb.setMessage(getString(R.string.ssh_info_tutorial_msg));
				    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	public void onClick(DialogInterface dialog, int which) {
				    		if (!showAgain2.isChecked()) {
				    			YTD.settings.edit().putBoolean("ssh_info", false).apply();
				    			Utils.logger("d", "sshInfoCheckboxEnabled: " + false, DEBUG_TAG);
				    		}
				    		callConnectBot(); 
			    		}
			    	});
				    if (! ((Activity) ShareActivity.this).isFinishing()) {
	    	        	adb.show();
	        		}
			    } else {
			    	callConnectBot();
			    }
			} catch (IndexOutOfBoundsException e) {
				Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
			}
		}
	}
    
    private void callDownloadManager(final String link, final int position, final String nameOfVideo) {
    	BugSenseHandler.leaveBreadcrumb("callDownloadManager");
    	final String aExt = findAudioCodec();
    	
    	dtl = new DownloadTaskListener() {
    		
    		@Override
			public void preDownload(DownloadTask task) {
				long ID = task.getDownloadId();
				Utils.logger("d", "__preDownload on ID: " + ID, DEBUG_TAG);
				
				Maps.mNetworkSpeedMap.put(ID, (long) 0);
				
				Json.addEntryToJsonFile(
						sShare, 
						String.valueOf(ID), 
						YTD.JSON_DATA_TYPE_V, 
						videoId,
						pos, 
						YTD.JSON_DATA_STATUS_IN_PROGRESS, 
						path.getAbsolutePath(), 
						nameOfVideo, 
						mComposedName, 
						aExt, 
						"-", 
						false);
				
				writeThumbToDisk();
				
				if (!autoModeEnabled) YTD.sequence.add(ID);
				
				YTD.NotificationHelper(sShare);
			}
			
			@Override
			public void updateProcess(DownloadTask task) {				
				// nothing to do
			}
			
			@Override
			public void finishDownload(DownloadTask task) {
				long ID = task.getDownloadId();
				String nameOfVideo = task.getDownloadedFileName();
				Utils.logger("d", "__finishDownload on ID: " + ID, DEBUG_TAG);
				
				Utils.scanMedia(getApplicationContext(), 
						new String[] { path.getPath() + File.separator + nameOfVideo }, 
						new String[] {"video/*"});
				
				String size;
				try {
					long downloadTotalSize = Maps.mTotalSizeMap.get(ID);
					size = String.valueOf(Utils.MakeSizeHumanReadable(downloadTotalSize, false));
				} catch (NullPointerException e) {
					Utils.logger("w", "NPE getting finished download size for ID: " + ID, DEBUG_TAG);
					size = "-";
				}
				
				Json.addEntryToJsonFile(
						sShare, 
						String.valueOf(ID), 
						YTD.JSON_DATA_TYPE_V, 
						videoId, 
						pos, 
						YTD.JSON_DATA_STATUS_COMPLETED, 
						path.getPath(), 
						nameOfVideo, 
						mComposedName, 
						aExt, 
						size, 
						false);
				
				if (DashboardActivity.isDashboardRunning)
					DashboardActivity.refreshlist(DashboardActivity.sDashboardActivity);
				
				YTD.removeIdUpdateNotification(ID);
				
				YTD.videoinfo.edit().remove(String.valueOf(ID) + "_link").commit();
				//YTD.videoinfo.edit().remove(String.valueOf(ID) + "_position").commit();
				
				Maps.removeFromAllMaps(ID);
				
				// TODO Auto FFmpeg task
				if (YTD.settings.getBoolean("ffmpeg_auto_cb", false) && !autoFFmpegTaskAlreadySent) {
					Utils.logger("d", "autoFfmpeg enabled: enqueing task for id: " + ID, DEBUG_TAG);
					
					autoFFmpegTaskAlreadySent = true;
					
					String[] bitrateData = null;
					String brType = null;
					String brValue = null;
					
					String audioFileName;
					
					String extrType = YTD.settings.getString("audio_extraction_type", "conv");
					if (extrType.equals("conv")) {
						bitrateData = Utils.retrieveBitrateValuesFromPref(sShare);
						audioFileName = basename + "_" + bitrateData[0] + "-" + bitrateData[1] + ".mp3";
						brType = bitrateData[0];
						brValue = bitrateData[1];
					} else {
						audioFileName = basename + aExt;
						
					}
					
					File audioFile = new File(path.getPath(), audioFileName);
					
					if (!audioFile.exists()) { 
						File videoFileToConvert = new File(path.getPath(), vFilename);
						
						YTD.queueThread.enqueueTask(new FFmpegExtractAudioTask(
								sShare, 
								videoFileToConvert, audioFile, 
								brType, brValue, 
								String.valueOf(ID), 
								videoId, 
								pos), 0);
					}
				} else {
					Utils.logger("v", "Auto FFmpeg task for ID " + ID
							+ " not enabled OR already sent for this video", DEBUG_TAG);
				}
			}
			
			@Override
			public void errorDownload(DownloadTask task, Throwable error) {
				long ID = task.getDownloadId();
				String nameOfVideo = task.getDownloadedFileName();
				
				Utils.logger("w", "__errorDownload on ID: " + ID, DEBUG_TAG);
				
				Toast.makeText(sShare,  nameOfVideo + ": " + getString(R.string.download_failed), 
						Toast.LENGTH_SHORT).show();
				
				String status = YTD.JSON_DATA_STATUS_PAUSED;
				String size = "-";
				
				if (error != null && error.getMessage() != null) {
					Pattern httpPattern = Pattern.compile("http error code: (400|403|404|405|410|411)");
					Matcher httpMatcher = httpPattern.matcher(error.getMessage());
					if (httpMatcher.find()) {
						status = YTD.JSON_DATA_STATUS_FAILED;
						Utils.logger("w", httpMatcher.group(1) + " Client Error for ID: " + ID, DEBUG_TAG);
					}
				}

				try {
					Long bytes_downloaded = Maps.mDownloadSizeMap.get(ID);
					Long bytes_total = Maps.mTotalSizeMap.get(ID);
					String progress = String.valueOf(Maps.mDownloadPercentMap.get(ID));
					String readableBytesDownloaded = Utils.MakeSizeHumanReadable(bytes_downloaded, false);
					String readableBytesTotal = Utils.MakeSizeHumanReadable(bytes_total, false);
					String progressRatio = readableBytesDownloaded + "/" + readableBytesTotal;
					size = progressRatio + " (" + progress + "%)";
				} catch (NullPointerException e) {
					Utils.logger("w", "errorDownload: NPE @ DM Maps", DEBUG_TAG);
				}
				
				Json.addEntryToJsonFile(
						sShare, 
						String.valueOf(ID), 
						YTD.JSON_DATA_TYPE_V, 
						videoId, 
						pos, 
						status, 
						path.getPath(), 
						nameOfVideo, 
						mComposedName, 
						aExt, 
						size, 
						false);
				
				if (DashboardActivity.isDashboardRunning)
					DashboardActivity.refreshlist(DashboardActivity.sDashboardActivity);
				
				YTD.removeIdUpdateNotification(ID);
			}
		};
		
    	//TODO
		File dest = new File(path, vFilename);
		File destTemp = new File(path, vFilename + DownloadTask.TEMP_SUFFIX);
		String previousJson = Json.readJsonDashboardFile(sShare);
		
		boolean blockDashboardLaunch = false;
		
		if (dest.exists() || (destTemp.exists() && previousJson.contains(dest.getName())) && !autoModeEnabled && !restartModeEnabled) {
			blockDashboardLaunch = true;
			PopUps.showPopUp(getString(R.string.long_press_warning_title), 
    				getString(R.string.menu_import_double), "info", ShareActivity.this);
		} else {
			long id = 0;
			if (autoModeEnabled || restartModeEnabled) {
				id = Long.parseLong(extraId);
			} else {
				id = System.currentTimeMillis();
			}
			
	        try {
				DownloadTask dt = new DownloadTask(this, id, link, vFilename, path.getPath(), dtl, false);
				YTD.videoinfo.edit().putString(String.valueOf(id) + "_link", link).apply();
				//YTD.videoinfo.edit().putInt(String.valueOf(id) + "_position", position).apply();
				Maps.dtMap.put(id, dt);
				dt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} catch (MalformedURLException e) {
				Log.e(DEBUG_TAG, "unable to start Download Manager -> " + e.getMessage());
			}
		}
		
		if (autoModeEnabled && !blockDashboardLaunch) {
			launchDashboardActivity();
		}
    }
    
    private String findAudioCodec() {
    	String aExt = null;
    	
		if (codecs.get(pos).equals("webm")) aExt = ".ogg";
	    if (codecs.get(pos).equals("mp4")) aExt = ".aac";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("small")) aExt = ".mp3";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("medium")) aExt = ".aac";
	    if (codecs.get(pos).equals("flv") && qualities.get(pos).equals("large")) aExt = ".aac";
	    if (codecs.get(pos).equals("3gp")) aExt = ".aac";

    	return aExt;
    }

    private String urlBlockMatchAndDecode(String content) {
    	
    	// log entire YouTube requests
    	//File req = new File(YTD.dir_Downloads, "YTD_yt_req.txt");
    	//Utils.appendStringToFile(req, content);
		
		if (asyncDownload.isCancelled()) {
			Utils.logger("d", "asyncDownload cancelled @ urlBlockMatchAndDecode begin", DEBUG_TAG);
			return "Cancelled!";
		}
		
		Pattern rtmpePattern = Pattern.compile("rtmpe=yes|conn=rtmpe");
		Matcher rtmpeMatcher = rtmpePattern.matcher(content);
		if (rtmpeMatcher.find()) {
			return "rtmpe";
		}
        
        Pattern loginPattern = Pattern.compile("restrictions:age");
        Matcher loginMatcher = loginPattern.matcher(content);
        if (loginMatcher.find()) {
        	return "login_required";
        }
        
        findVideoFilenameBase(content);
        findJs(content);

        int ue = matchUrlEncodedStreams(content);
        int as;
        
        boolean asEnabled = YTD.settings.getBoolean("enable_adaptive", false);
        if (asEnabled || autoModeEnabled) {
        	as = matchAdaptiveStreams(content);
        } else {
        	as = 0;
        }
        
        if ((ue + as) > 0) {
        	return "ok";
        } else {
        	return "e";
        }
    }

	private int matchUrlEncodedStreams(String content) {
		Pattern streamsPattern = Pattern.compile("url_encoded_fmt_stream_map\\\": \\\"(.*?)\\\"");
        Matcher streamsMatcher = streamsPattern.matcher(content);
        if (streamsMatcher.find()) {
        	Pattern blockPattern = Pattern.compile(",");
            Matcher blockMatcher = blockPattern.matcher(streamsMatcher.group(1));
            if (blockMatcher.find() && !asyncDownload.isCancelled()) {
            	String[] ueBlocks = streamsMatcher.group(1).split(blockPattern.toString());
            	int count = ueBlocks.length-1;
                Utils.logger("d", "*** url encoded streams ***", DEBUG_TAG);
                progressBar1.setIndeterminate(false);
                decryptionArray = null;
                while ((ueIndex+1) < count) {
                	try {
						ueBlocks[ueIndex] = URLDecoder.decode(ueBlocks[ueIndex], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.e(DEBUG_TAG, "UnsupportedEncodingException @ urlBlockMatchAndDecode: " + e.getMessage());
					}
                	
                	asyncDownload.doProgress((int) ((ueIndex / (float) count) * 100));
                	
                	Utils.logger("v", "block " + ueIndex + ": " + ueBlocks[ueIndex], DEBUG_TAG);
                	
                    codecMatcher(ueBlocks[ueIndex]);
                    qualityMatcher(ueBlocks[ueIndex]);
                    itagMatcher(ueBlocks[ueIndex]);
                    linkComposer(ueBlocks[ueIndex]);
                    
                    ueIndex++;
                }
            } else {
            	Utils.logger("d", "asyncDownload cancelled @ 'matchUrlEncodedStreams' match", DEBUG_TAG);
            }
            
            return 1;
        } else {
            return 0;
        }
	}
    
	private int matchAdaptiveStreams(String content) {
		Pattern streamsPattern = Pattern.compile("adaptive_fmts\\\": \\\"(.*?)\\\"");
        Matcher streamsMatcher = streamsPattern.matcher(content);
        if (streamsMatcher.find()) {
        	Pattern blockPattern = Pattern.compile(",");
            Matcher blockMatcher = blockPattern.matcher(streamsMatcher.group(1));
            if (blockMatcher.find() && !asyncDownload.isCancelled()) {
            	String[] asBlocks = streamsMatcher.group(1).split(blockPattern.toString());
            	int count = asBlocks.length-1;
                Utils.logger("d", "*** adaptive streams ***", DEBUG_TAG);
                //int asIndex = 0;
                while ((asIndex+1) < count) {
                	try {
						asBlocks[asIndex] = URLDecoder.decode(asBlocks[asIndex], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.e(DEBUG_TAG, "UnsupportedEncodingException @ urlBlockMatchAndDecode: " + e.getMessage());
					}
                	
                	asyncDownload.doProgress((int) ((asIndex / (float) count) * 100));
                	
                	Utils.logger("v", "block " + asIndex + ": " + asBlocks[asIndex], DEBUG_TAG);
                	
                	codecMatcher(asBlocks[asIndex]);
                    qualityMatcher(asBlocks[asIndex]);
                	itagMatcher(asBlocks[asIndex]);
                    linkComposer(asBlocks[asIndex]);
                    
                    asIndex++;
                }
            } else {
            	Utils.logger("d", "asyncDownload cancelled @ 'matchAdaptiveStreams' match", DEBUG_TAG);
            }
            
            return 1;
        } else {
            return 0;
        }
	}
	
    private class AsyncSizesFiller extends AsyncTask<String, String, Void> {

    	protected void onPreExecute() {
    		isAsyncSizesFillerRunning = true;
    		Utils.logger("d", "*** sizes ***", DEBUG_TAG);
    	}

		@Override
		protected Void doInBackground(String... urls) {
			for (int i = 0; i < urls.length; i++) {
				if (!this.isCancelled()) {
					String size = getVideoFileSize(urls[i]);
					if (size.equals("-")) {
						Utils.logger("w", "trying getVideoFileSize 2nd time", DEBUG_TAG);
						size = getVideoFileSize(urls[i]);
					}
					Utils.logger("d", "index: " + i + ", size: " + size, DEBUG_TAG);

					publishProgress(String.valueOf(i), size);
				}
			}
			return null;
		}
    	
    	protected void onProgressUpdate(String... i) {
    		Integer index = Integer.valueOf(i[0]);
    		String newValue = i[1];
    		
			sizes.remove(index);
			sizes.add(index, " - " + newValue);
    		
    		listEntries.clear();
    		listEntriesBuilder();

			aA.notifyDataSetChanged();
    	}

		@Override
		protected void onPostExecute(Void unused) {
			Utils.logger("v", "AsyncSizesFiller # onPostExecute", DEBUG_TAG);
			isAsyncSizesFillerRunning = false;
		}
    }

	private void findVideoFilenameBase(String content) {
        Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            titleRaw = titleMatcher.group().replaceAll("(<| - YouTube</)title>", "");
            titleRaw = android.text.Html.fromHtml(titleRaw).toString();
            basename = titleRaw.replaceAll("\\W", "_");
        } else {
            basename = "Youtube Video";
        }
        Utils.logger("d", "findVideoFilenameBase: " + basename, DEBUG_TAG);
    }
    
    private void listEntriesBuilder() {
    	Iterator<String> sizesIter = sizes.iterator();
        Iterator<String> itagsIter = itags.iterator();
        while (itagsIter.hasNext()) {        	
        	try {
				listEntries.add(itagsIter.next() + sizesIter.next());
        	} catch (NoSuchElementException e) {
        		listEntries.add("//");
        	}
        }
    }
    
    private void linkComposer(String block) {
    	int i = ueIndex + asIndex;
    	
    	Pattern urlPattern = Pattern.compile("url=(.+?)\\\\u0026");
    	Matcher urlMatcher = urlPattern.matcher(block);
    	String url = null;
		if (urlMatcher.find()) {
    		url = urlMatcher.group(1);
    	} else {
    		Pattern urlPattern2 = Pattern.compile("url=(.+?)$");
    		Matcher urlMatcher2 = urlPattern2.matcher(block);
    		if (urlMatcher2.find()) {
        		url = urlMatcher2.group(1);
        	} else {
        		Log.e(DEBUG_TAG, "index: " + i + "url: " + url);
        	}
    	}

		String sig = null;
		Pattern sigPattern = Pattern.compile("sig=(.+?)\\\\u0026");
    	Matcher sigMatcher = sigPattern.matcher(block);
		if (sigMatcher.find()) {
    		sig = "signature=" + sigMatcher.group(1);
    		Utils.logger("d", "index: " + i + ", non-ecrypted signature found on step 1", DEBUG_TAG);
    	} else {
    		Pattern sigPattern2 = Pattern.compile("sig=(.+?)$");
    		Matcher sigMatcher2 = sigPattern2.matcher(block);
    		if (sigMatcher2.find()) {
    			sig = "signature=" + sigMatcher2.group(1);
    			Utils.logger("d", "index: " + i + ", non-ecrypted signature found on step 2", DEBUG_TAG);
        	} else {
        		Pattern sigPattern3 = Pattern.compile("sig=([[0-9][A-Z]]{39,40}\\.[[0-9][A-Z]]{39,40})");
        		Matcher sigMatcher3 = sigPattern3.matcher(block);
        		if (sigMatcher3.find()) {
        			sig = "signature=" + sigMatcher3.group(1);
        			Utils.logger("d", "index: " + i + ", non-ecrypted signature found on step 3", DEBUG_TAG);
        		} else {
        			Pattern sigPattern4 = Pattern.compile("^s=(.+?)\\\\u0026");
        			Matcher sigMatcher4 = sigPattern4.matcher(block);
        			if (sigMatcher4.find()) {
        				Utils.logger("d", "index: " + i + ", encrypted signature found on step 1; length is " + sigMatcher4.group(1).length(), DEBUG_TAG);
        				sig = "signature=" + decryptExpSig(sigMatcher4.group(1));
        			} else {
        				Pattern sigPattern5 = Pattern.compile("\\\\u0026s=(.+?)\\\\u0026");
        	    		Matcher sigMatcher5 = sigPattern5.matcher(block);
        	    		if (sigMatcher5.find()) {
        	    			Utils.logger("d", "index: " + i + ", encrypted signature found on step 2; length is " + sigMatcher5.group(1).length(), DEBUG_TAG);
        	    			sig = "signature=" + decryptExpSig(sigMatcher5.group(1));
        	    		} else {
        	    			Pattern sigPattern6 = Pattern.compile("\\\\u0026s=(.+?)$");
                			Matcher sigMatcher6 = sigPattern6.matcher(block);
                			if (sigMatcher6.find()) {
                				Utils.logger("d", "index: " + i + ", encrypted signature found on step 3; length is " + sigMatcher6.group(1).length(), DEBUG_TAG);
                				sig = "signature=" + decryptExpSig(sigMatcher6.group(1));
		        			} else {
		        				Utils.logger("w", "index: " + i + ", sig: " + sig, DEBUG_TAG);
		        			}
        	    		}
        			}
        		}
        	}
    	}

		Utils.logger("v", "url " + i + ": " + url, DEBUG_TAG);
		Utils.logger("v", "sig " + i + ": " + sig, DEBUG_TAG);
    	
		String composedLink = url + "&" + sig;

		links.add(composedLink);
		//Utils.logger("i", composedLink);
		
		sizes.add("");
	}
    
    private String decryptExpSig(String sig) {
    	FetchUrl fu = new FetchUrl(sShare);
    	
    	if (decryptionArray == null) {
    		decryptionRule = null;
    		String jsCode = null;
			if (!jslink.equals("e")) {
				jsCode = fu.doFetch(jslink);
			} else {
				jsCode = fu.doFetch("https://s.ytimg.com/yts/jsbin/html5player-vflW444Sr.js");
			}
			String findSignatureCode = 
					"function isInteger(n) {" +
					"	return (typeof n==='number' && n%1==0);" +
					"}" +

					"function findSignatureCode(sourceCode) {" +
					"	var functionNameMatches=sourceCode.match(/\\.signature\\s*=\\s*(\\w+)\\(\\w+\\)/);" +
					"	var functionName=(functionNameMatches)?functionNameMatches[1]:null;" +
					"	" +
					"	var regCode=new RegExp('function '+functionName+" +
					"			'\\\\s*\\\\(\\\\w+\\\\)\\\\s*{\\\\w+=\\\\w+\\\\.split\\\\(\"\"\\\\);(.+);return \\\\w+\\\\.join');" +
					"	var functionCodeMatches=sourceCode.match(regCode);" +
					"	var functionCode=(functionCodeMatches)?functionCodeMatches[1]:null;" +
					"	" +
					"	var regSlice=new RegExp('slice\\\\s*\\\\(\\\\s*(.+)\\\\s*\\\\)');" +
					"	var regSwap=new RegExp('\\\\w+\\\\s*\\\\(\\\\s*\\\\w+\\\\s*,\\\\s*([0-9]+)\\\\s*\\\\)');" +
					"	var regInline=new RegExp('\\\\w+\\\\[0\\\\]\\\\s*=\\\\s*\\\\w+\\\\[([0-9]+)\\\\s*%\\\\s*\\\\w+\\\\.length\\\\]');" +
					"	var functionCodePieces = functionCode.split(';');" +
					"	var decodeArray=[], signatureLength=81;" +
					"	for (var i=0; i<functionCodePieces.length; i++) {" +
					"		functionCodePieces[i]=functionCodePieces[i].trim();" +
					"		if (functionCodePieces[i].length==0) {" +
					"		} else if (functionCodePieces[i].indexOf('slice') >= 0) {" +
					"			var sliceMatches=functionCodePieces[i].match(regSlice);" +
					"			var slice=(sliceMatches)?sliceMatches[1]:null;" +
					"			slice=parseInt(slice, 10);" +
					"			if (isInteger(slice)){ " +
					"				decodeArray.push(-slice);" +
					"				signatureLength+=slice;" +
					"			} " +
					"		} else if (functionCodePieces[i].indexOf('reverse') >= 0) {" +
					"			decodeArray.push(0);" +
					"		} else if (functionCodePieces[i].indexOf('[0]') >= 0) {" +
					"			if (i+2<functionCodePieces.length && " +
					" 				functionCodePieces[i+1].indexOf('.length') >= 0 &&" +					
					"				functionCodePieces[i+1].indexOf('[0]') >= 0) {" +
					"				var inlineMatches=functionCodePieces[i+1].match(regInline);" +
					"				var inline=(inlineMatches)?inlineMatches[1]:null;" +
					"				inline=parseInt(inline, 10);" +
					"				decodeArray.push(inline);" +
					"				i+=2;" +
					"			} " +
					"		} else if (functionCodePieces[i].indexOf(',') >= 0) {" +
					"			var swapMatches=functionCodePieces[i].match(regSwap);" +
					"			var swap=(swapMatches)?swapMatches[1]:null;" +
					"			swap=parseInt(swap, 10);" +
					"			if (isInteger(swap)){" +
					"				decodeArray.push(swap);" +
					"			} " +
					"		}" +
					"	}" +
					"	return decodeArray;" +
					"}";
	    	
			decryptionArray = RhinoRunner.obtainDecryptionArray(jsCode, findSignatureCode);
			decryptionFunction = "function decryptSignature(a){ a=a.split(\"\"); ";
			
			for (int i = 0; i < decryptionArray.length; i++) {
				//Utils.logger("i", "decryptionArray: " + decryptionArray[i], DEBUG_TAG);
				if (i == 0) {
					decryptionRule = decryptionArray[i];
				} else {
					decryptionRule = decryptionRule + "," + decryptionArray[i];
				}
				
				int rule = Integer.parseInt(decryptionArray[i]);
				
				if (rule == 0) decryptionFunction = decryptionFunction + "a=a.reverse(); ";
				if (rule < 0) decryptionFunction = decryptionFunction + "a=a.slice("+ -rule +"); ";
				if (rule > 0) decryptionFunction = decryptionFunction + "a=swap(a,"+ rule +"); ";
			}
			decryptionFunction = decryptionFunction + "return a.join(\"\")} function swap(a,b){ var c=a[0]; a[0]=a[b%a.length]; a[b]=c; return a };";
			
			Utils.logger("i", "decryptionRule (lenght is " + decryptionArray.length + "): " + decryptionRule, DEBUG_TAG);
			Utils.logger("i", "decryptionFunction: " + decryptionFunction, DEBUG_TAG);
		}
    	
    	String signature = RhinoRunner.decipher(sig, decryptionFunction);
    	
    	/*if (signature == sig || signature.isEmpty() || signature == null) {
    		String decryptSignatureLinkAtSf = 
    				"http://sourceforge.net/projects/ytdownloader/files/utils/decryptSignature/download";
    		Utils.logger("w", "signature empty, null or not deciphered" +
    				"\n -> falling back on JS function from " + decryptSignatureLinkAtSf, DEBUG_TAG);
    		
    		String decryptFunction2 = fu.doFetch(decryptSignatureLinkAtSf);
    		signature = RhinoRunner.decipher2(sig, decryptionRule, decryptFunction2);
    	}*/
		
		return signature;
	}
    
    private void findJs(String content) {
    	Pattern jsPattern = Pattern.compile("\"js\":\\s*\"([^\"]+)\"");
        Matcher jsMatcher = jsPattern.matcher(content);
        if (jsMatcher.find()) {
            jslink = jsMatcher.group(1).replaceAll("\\\\", "");
        } else {
            jslink = "e";
        }
        Utils.logger("v", "jslink: " + jslink, DEBUG_TAG);
    }

	private String getVideoFileSize(String link) {
		try {
			final URL url = new URL(link);
			URLConnection ucon = url.openConnection();
			ucon.connect();
			int file_size = ucon.getContentLength();
			return Utils.MakeSizeHumanReadable(file_size, false);
		} catch(IOException e) {
			return "-";
		}
	}

    private void codecMatcher(String current) {
        Pattern codecPattern = Pattern.compile("(webm|mp4|flv|3gp)");
        Matcher codecMatcher = codecPattern.matcher(current);
        if (codecMatcher.find()) {
            codecs.add(codecMatcher.group());
        } else {
            codecs.add("video");
        }
        int i = ueIndex + asIndex;
        Utils.logger("d", "index: " + i + ", Codec: " + codecs.get(i), DEBUG_TAG);
    }

    private void qualityMatcher(String current) {
        Pattern qualityPattern = Pattern.compile("(highres|hd1080|hd720|large|medium|small)");
        Matcher qualityMatcher = qualityPattern.matcher(current);
        if (qualityMatcher.find()) {
            qualities.add(qualityMatcher.group().replace("highres", "Original"));
        } else {
            qualities.add("-");
        }
        int i = ueIndex + asIndex;
        Utils.logger("d", "index: " + i + ", Quality: " + qualities.get(i), DEBUG_TAG);
    }
    
    /*private void stereoMatcher(String current, int i) {
        Pattern qualityPattern = Pattern.compile("stereo3d=1");
        Matcher qualityMatcher = qualityPattern.matcher(current);
        if (qualityMatcher.find()) {
            stereo.add(qualityMatcher.group().replace("stereo3d=1", "_3D"));
        } else {
            stereo.add("");
        }
        //Utils.logger("d", "index: " + i + ", Quality: " + qualities.get(i), DEBUG_TAG);
    }*/
    
    private void itagMatcher(String current) {
    	String res = "-";
    	int i = ueIndex + asIndex;
    	Pattern itagPattern = Pattern.compile("itag=([0-9]{1,3})\\\\u0026");
    	Matcher itagMatcher = itagPattern.matcher(current);
    	if (itagMatcher.find()) {
    		res = findItag(itagMatcher, res);
    		Utils.logger("d", "index: " + i + ", itag: " + itagMatcher.group(1) + " (" + res + ")", DEBUG_TAG);
    	} else {
    		Pattern itagPattern2 = Pattern.compile("itag=([0-9]{1,3})$");
        	Matcher itagMatcher2 = itagPattern2.matcher(current);
	    	if (itagMatcher2.find()) {
	    		res = findItag(itagMatcher2, res);
	    		Utils.logger("d", "index: " + i + ", itag: " + itagMatcher2.group(1) + " (" + res + ")", DEBUG_TAG);
	    	}
    	}
    	itags.add(res);
    }

	private String findItag(Matcher itagMatcher, String res) {
		String itag = itagMatcher.group(1);
		if (itag != null) {
			try {
				switch (Integer.parseInt(itag)) {
				// ***************************
				// *** url encoded streams ***
				// ***************************
				case 5:
					res = "FLV - 240p";
					break;
				case 6:
					res = "FLV - 270p";
					break;
				case 17:
					res = "3GP - 144p";
					break;
				case 18:
					res = "MP4 - 270p/360p";
					break;
				case 22:
					res = "MP4 - 720p";
					break;
				case 34:
					res = "FLV - 360p";
					break;
				case 35:
					res = "FLV - 480p";
					break;
				case 36:
					res = "3GP - 240p";
					break;
				case 37:
					res = "MP4 - 1080p";
					break;
				case 38:
					res = "MP4 - Original";
					break;
				case 43:
					res = "WebM - 360p";
					break;
				case 44:
					res = "WebM - 480p";
					break;
				case 45:
					res = "WebM - 720p";
					break;
				case 46:
					res = "WebM - 1080p";
					break;
				case 82:
					res = "MP4 - 360p - 3D";
					break;
				case 83:
					res = "MP4 - 240p - 3D";
					break;
				case 84:
					res = "MP4 - 720p - 3D";
					break;
				case 85:
					res = "MP4 - 520p - 3D";
					break;
				case 100:
					res = "WebM - 360p - 3D";
					break;
				case 101:
					res = "WebM - 360p - 3D";
					break;
				case 102:
					res = "WebM - 720p - 3D";
					break;
				// ************************
				// *** adaptive streams ***
				// ************************
				case 133:
					res = "VO - MP4 - 240p";
					break;
				case 134:
					res = "VO - MP4 - 360p";
					break;
				case 135:
					res = "VO - MP4 - 480p";
					break;
				case 136:
					res = "VO - MP4 - 720p";
					break;
				case 137:
					res = "VO - MP4 - 1080p";
					break;
				case 139:
					res = "AO - MP4 - Low-Q";
					break;
				case 140:
					res = "AO - MP4 - Med-Q";
					break;
				case 141:
					res = "AO - MP4 - Hi-Q";
					break;
				case 160:
					res = "VO - MP4 - 144p";
					break;
				case 171:
					res = "AO - WebM - Med-Q";
					break;
				case 172:
					res = "AO - WebM - Hi-Q";
					break;
				case 242:
					res = "VO - WebM - 240p";
					break;
				case 243:
					res = "VO - WebM - 360p";
					break;
				case 244:
					res = "VO - WebM - 480p";
					break;
				case 245:
					res = "VO - WebM - 480p";
					break;
				case 246:
					res = "VO - WebM - 480p";
					break;
				case 247:
					res = "VO - WebM - 720p";
					break;
				case 248:
					res = "VO - WebM - 1080p";
					break;
				}
				
			} catch (NumberFormatException e) {
				Log.e(DEBUG_TAG, "resolutionMatcher --> " + e.getMessage());
			}
		}
		return res;
	}
    
    private String[] generateThumbUrls() {
    	
    	String url1 = "http://i1.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	String url2 = "http://i2.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	String url3 = "http://i3.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	String url4 = "http://i4.ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	
    	String[] urls = { url1, url2, url3, url4 };
    	return urls;
	}
    
    private Bitmap downloadThumbnail(String fileUrl) {
    	InputStream is = null;
    	URL myFileUrl = null;
    	try {
    		myFileUrl = new URL(fileUrl);
    		HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
    		conn.setDoInput(true);
    		conn.connect();
    		is = conn.getInputStream();
    		return BitmapFactory.decodeStream(is);
    	} catch (IOException e) {
    		Log.e(DEBUG_TAG, "IOException @ " + e.getMessage());
            return null;
		}
    }
    
    private void assignBitmapToVideoListThumbnail(String[] url) {
    	Bitmap bm0  = downloadThumbnail(url[0]);
    	if (bm0 != null) {
    		img = bm0;
    		Utils.logger("d", "assigning bitmap from url[0]: " + url[0], DEBUG_TAG);
    	} else {
    		Bitmap bm1  = downloadThumbnail(url[1]);
    		if (bm1 != null) {
        		img = bm1;
        		Utils.logger("d", "assigning bitmap from url[1]: " + url[1], DEBUG_TAG);
        	} else {
        		Bitmap bm2  = downloadThumbnail(url[2]);
        		if (bm2 != null) {
            		img = bm2;
            		Utils.logger("d", "assigning bitmap from url[2]: " + url[2], DEBUG_TAG);
            	} else {
            		Bitmap bm3  = downloadThumbnail(url[3]);
            		if (bm3 != null) {
            			img = bm3;
            			Utils.logger("d", "assigning bitmap from url[3]: " + url[3], DEBUG_TAG);
            		} else {
            			Log.e(DEBUG_TAG, "\nFalling back on asset's placeholder");
                		InputStream assIs = null;
                		AssetManager assMan = getAssets();
                        try {
            				assIs = assMan.open("placeholder.png");
            			} catch (IOException e1) {
            				Log.e(DEBUG_TAG, "downloadThumbnail -> " + e1.getMessage());
            			}
                        img = BitmapFactory.decodeStream(assIs);
            		}
            	}
        	}
    	}
    }
    
    private void writeThumbToDisk() {
    	File thumbFile = new File(sShare.getDir(YTD.THUMBS_FOLDER, 0), videoId + ".png");
		//if (!thumbFile.exists()) {
			try {
				FileOutputStream os = new FileOutputStream(thumbFile);
				img.compress(Bitmap.CompressFormat.PNG, 50, os);
			} catch (FileNotFoundException e) {
				Log.e(DEBUG_TAG, "writeThumbToDisk -> " + e.getMessage());
			}
		//}
    }
    
    private void updateInit() {
		int prefSig = YTD.settings.getInt("APP_SIGNATURE", 0);
		Utils.logger("d", "prefSig: " + prefSig, DEBUG_TAG);
		
		if (prefSig == SettingsActivity.SettingsFragment.YTD_SIG_HASH) {
				Utils.logger("d", "YTD signature in PREFS: update check possile", DEBUG_TAG);
				
				if (YTD.settings.getBoolean("autoupdate", false)) {
					Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
					SettingsActivity.SettingsFragment.autoUpdate(ShareActivity.this);
				}
		} else {
			Utils.logger("d", "different or null YTD signature. Update check cancelled.", DEBUG_TAG);
		}
	}
}
