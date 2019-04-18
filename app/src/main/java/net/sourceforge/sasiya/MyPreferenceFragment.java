package net.sourceforge.sasiya;

import net.sourceforge.sasiya.CameraController.CameraController;
import net.sourceforge.sasiya.Preview.Preview;
import net.sourceforge.sasiya.UI.FolderChooserDialog;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/** Fragment to handle the Settings UI. Note that originally this was a
 *  PreferenceActivity rather than a PreferenceFragment which required all
 *  communication to be via the bundle (since this replaced the MainActivity,
 *  meaning we couldn't access data from that class. This no longer applies due
 *  to now using a PreferenceFragment, but I've still kept with transferring
 *  information via the bundle (for the most part, at least).
 *  Also note that passing via a bundle may be necessary to avoid accessing the
 *  preview, which can be null - see note about video resolutions below.
 *  Also see https://stackoverflow.com/questions/14093438/after-the-rotate-oncreate-fragment-is-called-before-oncreate-fragmentactivi .
 */
public class MyPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "MyPreferenceFragment";

	private int cameraId;

	/* Any AlertDialogs we create should be added to dialogs, and removed when dismissed. Any dialogs still
	 * opened when onDestroy() is called are closed.
	 * Normally this shouldn't be needed - the settings is usually only closed by the user pressing Back,
	 * which can only be done once any opened dialogs are also closed. But this is required if we want to
	 * programmatically close the settings - this is done in MainActivity.onNewIntent(), so that if Open Camera
	 * is launched from the homescreen again when the settings was opened, we close the settings.
	 * UPDATE: At the time of writing, we don't set android:launchMode="singleTask", so onNewIntent() is not called,
	 * so this code isn't necessary - but there shouldn't be harm to leave it here for future use.
	 */
	private final HashSet<AlertDialog> dialogs = new HashSet<>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(MyDebug.LOG)
			Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final Bundle bundle = getArguments();
		this.cameraId = bundle.getInt("cameraId");
		if(MyDebug.LOG)
			Log.d(TAG, "cameraId: " + cameraId);
		final int nCameras = bundle.getInt("nCameras");
		if(MyDebug.LOG)
			Log.d(TAG, "nCameras: " + nCameras);

		final String camera_api = bundle.getString("camera_api");
		
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

		final boolean supports_auto_stabilise = bundle.getBoolean("supports_auto_stabilise");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_auto_stabilise: " + supports_auto_stabilise);

		/*if( !supports_auto_stabilise ) {
			Preference pref = findPreference("preference_auto_stabilise");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_camera_effects");
        	pg.removePreference(pref);
		}*/

		//readFromBundle(bundle, "color_effects", Preview.getColorEffectPreferenceKey(), Camera.Parameters.EFFECT_NONE, "preference_category_camera_effects");
		//readFromBundle(bundle, "scene_modes", Preview.getSceneModePreferenceKey(), Camera.Parameters.SCENE_MODE_AUTO, "preference_category_camera_effects");
		//readFromBundle(bundle, "white_balances", Preview.getWhiteBalancePreferenceKey(), Camera.Parameters.WHITE_BALANCE_AUTO, "preference_category_camera_effects");
		//readFromBundle(bundle, "isos", Preview.getISOPreferenceKey(), "auto", "preference_category_camera_effects");
		//readFromBundle(bundle, "exposures", "preference_exposure", "0", "preference_category_camera_effects");


		final boolean supports_face_detection = bundle.getBoolean("supports_face_detection");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_face_detection: " + supports_face_detection);

		if( !supports_face_detection ) {
			Preference pref = findPreference("preference_face_detection");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_camera_controls");
        	pg.removePreference(pref);
		}

		final int preview_width = bundle.getInt("preview_width");
		final int preview_height = bundle.getInt("preview_height");
		final int [] preview_widths = bundle.getIntArray("preview_widths");
		final int [] preview_heights = bundle.getIntArray("preview_heights");
		final int [] video_widths = bundle.getIntArray("video_widths");
		final int [] video_heights = bundle.getIntArray("video_heights");
		final int [] video_fps = bundle.getIntArray("video_fps");

		final int resolution_width = bundle.getInt("resolution_width");
		final int resolution_height = bundle.getInt("resolution_height");
		final int [] widths = bundle.getIntArray("resolution_widths");
		final int [] heights = bundle.getIntArray("resolution_heights");
		final boolean [] supports_burst = bundle.getBooleanArray("resolution_supports_burst");
		if( widths != null && heights != null && supports_burst != null ) {
			CharSequence [] entries = new CharSequence[widths.length];
			CharSequence [] values = new CharSequence[widths.length];
			for(int i=0;i<widths.length;i++) {
				entries[i] = widths[i] + " x " + heights[i] + " " + Preview.getAspectRatioMPString(getResources(), widths[i], heights[i], supports_burst[i]);
				values[i] = widths[i] + " " + heights[i];
			}
			ListPreference lp = (ListPreference)findPreference("preference_resolution");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String resolution_preference_key = PreferenceKeys.getResolutionPreferenceKey(cameraId);
			String resolution_value = sharedPreferences.getString(resolution_preference_key, "");
			if(MyDebug.LOG)
				Log.d(TAG, "resolution_value: " + resolution_value);
			lp.setValue(resolution_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(resolution_preference_key);
		}
		else {
			Preference pref = findPreference("preference_resolution");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		String fps_preference_key = PreferenceKeys.getVideoFPSPreferenceKey(cameraId);
		if(MyDebug.LOG)
			Log.d(TAG, "fps_preference_key: " + fps_preference_key);
		String fps_value = sharedPreferences.getString(fps_preference_key, "default");
		if(MyDebug.LOG)
			Log.d(TAG, "fps_value: " + fps_value);
		if( video_fps != null ) {
			// build video fps settings
			CharSequence [] entries = new CharSequence[video_fps.length+1];
			CharSequence [] values = new CharSequence[video_fps.length+1];
			int i=0;
			// default:
			entries[i] = getResources().getString(R.string.preference_video_fps_default);
			values[i] = "default";
			i++;
			for(int fps : video_fps) {
				entries[i] = "" + fps;
				values[i] = "" + fps;
				i++;
			}

			ListPreference lp = (ListPreference)findPreference("preference_video_fps");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			lp.setValue(fps_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(fps_preference_key);
		}

		{
			final int n_quality = 100;
			CharSequence [] entries = new CharSequence[n_quality];
			CharSequence [] values = new CharSequence[n_quality];
			for(int i=0;i<n_quality;i++) {
				entries[i] = "" + (i+1) + "%";
				values[i] = "" + (i+1);
			}
			ListPreference lp = (ListPreference)findPreference("preference_quality");
			lp.setEntries(entries);
			lp.setEntryValues(values);
		}
		
		final boolean supports_raw = bundle.getBoolean("supports_raw");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_raw: " + supports_raw);

		if( !supports_raw ) {
			Preference pref = findPreference("preference_raw");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}
		else {
        	ListPreference pref = (ListPreference)findPreference("preference_raw");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
	        	// RAW only mode requires at least Android 7; earlier versions seem to have poorer support for DNG files
	        	pref.setEntries(R.array.preference_raw_entries_preandroid7);
	        	pref.setEntryValues(R.array.preference_raw_values_preandroid7);
			}

        	pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        		@Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
            		if(MyDebug.LOG)
            			Log.d(TAG, "clicked raw: " + newValue);
            		if( newValue.equals("preference_raw_yes") || newValue.equals("preference_raw_only") ) {
            			// we check done_raw_info every time, so that this works if the user selects RAW again without leaving and returning to Settings
            			boolean done_raw_info = sharedPreferences.contains(PreferenceKeys.RawInfoPreferenceKey);
            			if( !done_raw_info ) {
	        		        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
	        	            alertDialog.setTitle(R.string.preference_raw);
	        	            alertDialog.setMessage(R.string.raw_info);
	        	            alertDialog.setPositiveButton(android.R.string.ok, null);
	        	            alertDialog.setNegativeButton(R.string.dont_show_again, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
				            		if(MyDebug.LOG)
				            			Log.d(TAG, "user clicked dont_show_again for raw info dialog");
				            		SharedPreferences.Editor editor = sharedPreferences.edit();
				            		editor.putBoolean(PreferenceKeys.RawInfoPreferenceKey, true);
				            		editor.apply();
								}
	        	            });
							final AlertDialog alert = alertDialog.create();
							// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
							alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface arg0) {
									if(MyDebug.LOG)
										Log.d(TAG, "raw dialog dismissed");
									dialogs.remove(alert);
								}
							});
							alert.show();
	        	            dialogs.add(alert);
            			}
                    }
                	return true;
                }
            });        	
		}

		final boolean supports_hdr = bundle.getBoolean("supports_hdr");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_hdr: " + supports_hdr);

		if( !supports_hdr ) {
			Preference pref = findPreference("preference_hdr_save_expo");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		final boolean supports_expo_bracketing = bundle.getBoolean("supports_expo_bracketing");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_expo_bracketing: " + supports_expo_bracketing);

		final int max_expo_bracketing_n_images = bundle.getInt("max_expo_bracketing_n_images");
		if(MyDebug.LOG)
			Log.d(TAG, "max_expo_bracketing_n_images: " + max_expo_bracketing_n_images);

		final boolean supports_nr = bundle.getBoolean("supports_nr");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_nr: " + supports_nr);

		if( !supports_nr ) {
			Preference pref = findPreference("preference_nr_save");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		final boolean supports_exposure_compensation = bundle.getBoolean("supports_exposure_compensation");
		final int exposure_compensation_min = bundle.getInt("exposure_compensation_min");
		final int exposure_compensation_max = bundle.getInt("exposure_compensation_max");
		if (MyDebug.LOG){
			Log.d(TAG, "supports_exposure_compensation: " + supports_exposure_compensation);
			Log.d(TAG, "exposure_compensation_min: " + exposure_compensation_min);
			Log.d(TAG, "exposure_compensation_max: " + exposure_compensation_max);
		}

		final boolean supports_iso_range = bundle.getBoolean("supports_iso_range");
		final int iso_range_min = bundle.getInt("iso_range_min");
		final int iso_range_max = bundle.getInt("iso_range_max");
		if (MyDebug.LOG){
			Log.d(TAG, "supports_iso_range: " + supports_iso_range);
			Log.d(TAG, "iso_range_min: " + iso_range_min);
			Log.d(TAG, "iso_range_max: " + iso_range_max);
		}

		final boolean supports_exposure_time = bundle.getBoolean("supports_exposure_time");
		final long exposure_time_min = bundle.getLong("exposure_time_min");
		final long exposure_time_max = bundle.getLong("exposure_time_max");
		if (MyDebug.LOG){
			Log.d(TAG, "supports_exposure_time: " + supports_exposure_time);
			Log.d(TAG, "exposure_time_min: " + exposure_time_min);
			Log.d(TAG, "exposure_time_max: " + exposure_time_max);
		}

		final boolean supports_white_balance_temperature = bundle.getBoolean("supports_white_balance_temperature");
		final int white_balance_temperature_min = bundle.getInt("white_balance_temperature_min");
		final int white_balance_temperature_max = bundle.getInt("white_balance_temperature_max");
		if (MyDebug.LOG){
			Log.d(TAG, "supports_white_balance_temperature: " + supports_white_balance_temperature);
			Log.d(TAG, "white_balance_temperature_min: " + white_balance_temperature_min);
			Log.d(TAG, "white_balance_temperature_max: " + white_balance_temperature_max);
		}

		if( !supports_expo_bracketing || max_expo_bracketing_n_images <= 3 ) {
			Preference pref = findPreference("preference_expo_bracketing_n_images");
			PreferenceGroup pg = (PreferenceGroup) this.findPreference("preference_screen_photo_settings");
			pg.removePreference(pref);
		}
		if( !supports_expo_bracketing ) {
			Preference pref = findPreference("preference_expo_bracketing_stops");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
		}

		/* Set up video resolutions.
		   Note that this will be the resolutions for either standard or high speed frame rate (where
		   the latter may also include being in slow motion mode), depending on the current setting when
		   this settings fragment is launched. A limitation is that if the user changes the fps value
		   within the settings, this list won't update until the user exits and re-enters the settings.
		   This could be fixed by setting a setOnPreferenceChangeListener for the preference_video_fps
		   ListPreference and updating, but we must not assume that the preview will be non-null (since
		   if the application is being recreated, MyPreferenceFragment.onCreate() is called via
		   MainActivity.onCreate()->super.onCreate() before the preview is created! So we still need to
		   read the info via a bundle, and only update when fps changes if the preview is non-null.
		 */
		final String [] video_quality = bundle.getStringArray("video_quality");
		final String [] video_quality_string = bundle.getStringArray("video_quality_string");
		if( video_quality != null && video_quality_string != null ) {
			CharSequence [] entries = new CharSequence[video_quality.length];
			CharSequence [] values = new CharSequence[video_quality.length];
			for(int i=0;i<video_quality.length;i++) {
				entries[i] = video_quality_string[i];
				values[i] = video_quality[i];
			}
			ListPreference lp = (ListPreference)findPreference("preference_video_quality");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String video_quality_preference_key = bundle.getString("video_quality_preference_key");
			if(MyDebug.LOG)
				Log.d(TAG, "video_quality_preference_key: " + video_quality_preference_key);
			String video_quality_value = sharedPreferences.getString(video_quality_preference_key, "");
			if(MyDebug.LOG)
				Log.d(TAG, "video_quality_value: " + video_quality_value);
			// set the key, so we save for the correct cameraId and high-speed setting
			// this must be done before setting the value (otherwise the video resolutions preference won't be
			// updated correctly when this is called from the callback when the user switches between
			// normal and high speed frame rates
			lp.setKey(video_quality_preference_key);
			lp.setValue(video_quality_value);
		}
		else {
			Preference pref = findPreference("preference_video_quality");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		final String current_video_quality = bundle.getString("current_video_quality");
		final int video_frame_width = bundle.getInt("video_frame_width");
		final int video_frame_height = bundle.getInt("video_frame_height");
		final int video_bit_rate = bundle.getInt("video_bit_rate");
		final int video_frame_rate = bundle.getInt("video_frame_rate");
		final double video_capture_rate = bundle.getDouble("video_capture_rate");
		final boolean video_high_speed = bundle.getBoolean("video_high_speed");
		final float video_capture_rate_factor = bundle.getFloat("video_capture_rate_factor");

		final boolean supports_force_video_4k = bundle.getBoolean("supports_force_video_4k");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_force_video_4k: " + supports_force_video_4k);
		if( !supports_force_video_4k || video_quality == null ) {
			Preference pref = findPreference("preference_force_video_4k");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_video_debugging");
        	pg.removePreference(pref);
		}
		
		final boolean supports_video_stabilization = bundle.getBoolean("supports_video_stabilization");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_video_stabilization: " + supports_video_stabilization);
		if( !supports_video_stabilization ) {
			Preference pref = findPreference("preference_video_stabilization");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		{
        	ListPreference pref = (ListPreference)findPreference("preference_record_audio_src");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
	        	// some values require at least Android 7
	        	pref.setEntries(R.array.preference_record_audio_src_entries_preandroid7);
	        	pref.setEntryValues(R.array.preference_record_audio_src_values_preandroid7);
			}
		}

		final boolean can_disable_shutter_sound = bundle.getBoolean("can_disable_shutter_sound");
		if(MyDebug.LOG)
			Log.d(TAG, "can_disable_shutter_sound: " + can_disable_shutter_sound);
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !can_disable_shutter_sound ) {
        	// Camera.enableShutterSound requires JELLY_BEAN_MR1 or greater
        	Preference pref = findPreference("preference_shutter_sound");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_camera_controls_more");
        	pg.removePreference(pref);
        }

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {
        	// Some immersive modes require KITKAT - simpler to require Kitkat for any of the menu options
        	Preference pref = findPreference("preference_immersive_mode");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_gui");
        	pg.removePreference(pref);
        }
        
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
        	// the required ExifInterface tags requires Android N or greater
        	Preference pref = findPreference("preference_category_exif_tags");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        	pg.removePreference(pref);
        }
        else {
			setSummary("preference_exif_artist");
			setSummary("preference_exif_copyright");
		}

		setSummary("preference_save_photo_prefix");
		setSummary("preference_save_video_prefix");
		setSummary("preference_textstamp");

		final boolean using_android_l = bundle.getBoolean("using_android_l");
		if(MyDebug.LOG)
			Log.d(TAG, "using_android_l: " + using_android_l);
		final boolean supports_photo_video_recording = bundle.getBoolean("supports_photo_video_recording");
		if(MyDebug.LOG)
			Log.d(TAG, "supports_photo_video_recording: " + supports_photo_video_recording);

        if( !using_android_l ) {
        	Preference pref = findPreference("preference_show_iso");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_gui");
        	pg.removePreference(pref);
        }

        if( !using_android_l ) {
        	Preference pref = findPreference("preference_camera2_fake_flash");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
        	pg.removePreference(pref);

			pref = findPreference("preference_camera2_fast_burst");
			pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			pg.removePreference(pref);

			pref = findPreference("preference_camera2_photo_video_recording");
			pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			pg.removePreference(pref);
        }
        else {
        	if( !supports_photo_video_recording ) {
				Preference pref = findPreference("preference_camera2_photo_video_recording");
				PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
				pg.removePreference(pref);
			}
		}

		final int tonemap_max_curve_points = bundle.getInt("tonemap_max_curve_points");
		final boolean supports_tonemap_curve = bundle.getBoolean("supports_tonemap_curve");
		if (MyDebug.LOG){
			Log.d(TAG, "tonemap_max_curve_points: " + tonemap_max_curve_points);
			Log.d(TAG, "supports_tonemap_curve: " + supports_tonemap_curve);
		}
        if( !supports_tonemap_curve ) {
        	Preference pref = findPreference("preference_video_log");
			PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_video_settings");
        	pg.removePreference(pref);
		}

		{
			// remove preference_category_photo_debugging category if empty (which will be the case for old api)
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_category_photo_debugging");
			if(MyDebug.LOG)
				Log.d(TAG, "preference_category_photo_debugging children: " + pg.getPreferenceCount());
        	if( pg.getPreferenceCount() == 0 ) {
        		// pg.getParent() requires API level 26
	        	PreferenceGroup parent = (PreferenceGroup)this.findPreference("preference_screen_photo_settings");
        		parent.removePreference(pg);
			}
		}


        {
        	ListPreference pref = (ListPreference)findPreference("preference_ghost_image");

	        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
	        	// require Storage Access Framework to select a ghost image
	        	pref.setEntries(R.array.preference_ghost_image_entries_preandroid5);
	        	pref.setEntryValues(R.array.preference_ghost_image_values_preandroid5);
			}

        	pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        		@Override
                public boolean onPreferenceChange(Preference arg0, Object newValue) {
            		if(MyDebug.LOG)
            			Log.d(TAG, "clicked ghost image: " + newValue);
            		if( newValue.equals("preference_ghost_image_selected") ) {
						MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
						main_activity.openGhostImageChooserDialogSAF(true);
					}
            		return true;
                }
            });
        }

        /*{
        	EditTextPreference edit = (EditTextPreference)findPreference("preference_save_location");
        	InputFilter filter = new InputFilter() { 
        		// whilst Android seems to allow any characters on internal memory, SD cards are typically formatted with FAT32
        		String disallowed = "|\\?*<\":>";
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) { 
                    for(int i=start;i<end;i++) { 
                    	if( disallowed.indexOf( source.charAt(i) ) != -1 ) {
                            return ""; 
                    	}
                    } 
                    return null; 
                }
        	}; 
        	edit.getEditText().setFilters(new InputFilter[]{filter});         	
        }*/
        {
        	Preference pref = findPreference("preference_save_location");
        	pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		@Override
                public boolean onPreferenceClick(Preference arg0) {
            		if(MyDebug.LOG)
            			Log.d(TAG, "clicked save location");
            		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
            		if( main_activity.getStorageUtils().isUsingSAF() ) {
                		main_activity.openFolderChooserDialogSAF(true);
            			return true;
                    }
            		else {
						FolderChooserDialog fragment = new SaveFolderChooserDialog();
                		fragment.show(getFragmentManager(), "FOLDER_FRAGMENT");
                    	return true;
            		}
                }
            });        	
        }

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
        	Preference pref = findPreference("preference_using_saf");
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference("preference_screen_camera_controls_more");
        	pg.removePreference(pref);
        }
        else {
            final Preference pref = findPreference("preference_using_saf");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_using_saf") ) {
                		if(MyDebug.LOG)
                			Log.d(TAG, "user clicked saf");
            			if( sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) ) {
                    		if(MyDebug.LOG)
                    			Log.d(TAG, "saf is now enabled");
                    		// seems better to alway re-show the dialog when the user selects, to make it clear where files will be saved (as the SAF location in general will be different to the non-SAF one)
                    		//String uri = sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");
                    		//if( uri.length() == 0 )
                    		{
                        		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
                    			Toast.makeText(main_activity, R.string.saf_select_save_location, Toast.LENGTH_SHORT).show();
                        		main_activity.openFolderChooserDialogSAF(true);
                    		}
            			}
            			else {
                    		if(MyDebug.LOG)
                    			Log.d(TAG, "saf is now disabled");
            			}
                	}
                	return false;
                }
            });
        }

		{
			final Preference pref = findPreference("preference_calibrate_level");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_calibrate_level") ) {
						if(MyDebug.LOG)
							Log.d(TAG, "user clicked calibrate level option");
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
						alertDialog.setTitle(getActivity().getResources().getString(R.string.preference_calibrate_level));
						alertDialog.setMessage(R.string.preference_calibrate_level_dialog);
						alertDialog.setPositiveButton(R.string.preference_calibrate_level_calibrate, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(MyDebug.LOG)
									Log.d(TAG, "user clicked calibrate level");
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								if( main_activity.getPreview().hasLevelAngleStable() ) {
									double current_level_angle = main_activity.getPreview().getLevelAngleUncalibrated();
									SharedPreferences.Editor editor = sharedPreferences.edit();
									editor.putFloat(PreferenceKeys.CalibratedLevelAnglePreferenceKey, (float)current_level_angle);
									editor.apply();
									main_activity.getPreview().updateLevelAngles();
									Toast.makeText(main_activity, R.string.preference_calibrate_level_calibrated, Toast.LENGTH_SHORT).show();
								}
							}
						});
						alertDialog.setNegativeButton(R.string.preference_calibrate_level_reset, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(MyDebug.LOG)
									Log.d(TAG, "user clicked reset calibration level");
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								SharedPreferences.Editor editor = sharedPreferences.edit();
								editor.putFloat(PreferenceKeys.CalibratedLevelAnglePreferenceKey, 0.0f);
								editor.apply();
								main_activity.getPreview().updateLevelAngles();
								Toast.makeText(main_activity, R.string.preference_calibrate_level_calibration_reset, Toast.LENGTH_SHORT).show();
							}
						});
						final AlertDialog alert = alertDialog.create();
						// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
						alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface arg0) {
								if(MyDebug.LOG)
									Log.d(TAG, "calibration dialog dismissed");
								dialogs.remove(alert);
							}
						});
						alert.show();
						dialogs.add(alert);
						return false;
					}
					return false;
				}
			});
		}

        {
            final Preference pref = findPreference("preference_reset");
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	if( pref.getKey().equals("preference_reset") ) {
                		if(MyDebug.LOG)
                			Log.d(TAG, "user clicked reset");
    				    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
			        	alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			        	alertDialog.setTitle(R.string.preference_reset);
			        	alertDialog.setMessage(R.string.preference_reset_question);
			        	alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			        		@Override
					        public void onClick(DialogInterface dialog, int which) {
		                		if(MyDebug.LOG)
		                			Log.d(TAG, "user confirmed reset");
		                		SharedPreferences.Editor editor = sharedPreferences.edit();
		                		editor.clear();
		                		editor.putBoolean(PreferenceKeys.FirstTimePreferenceKey, true);
								try {
									PackageInfo pInfo = MyPreferenceFragment.this.getActivity().getPackageManager().getPackageInfo(MyPreferenceFragment.this.getActivity().getPackageName(), 0);
			                        int version_code = pInfo.versionCode;
									editor.putInt(PreferenceKeys.LatestVersionPreferenceKey, version_code);
								}
								catch(NameNotFoundException e) {
									if(MyDebug.LOG)
										Log.d(TAG, "NameNotFoundException exception trying to get version number");
									e.printStackTrace();
								}
		                		editor.apply();
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								main_activity.setDeviceDefaults();
		                		if(MyDebug.LOG)
		                			Log.d(TAG, "user clicked reset - need to restart");
		                		restartSaSiYa();
								main_activity.openSettings();
					        }
			        	});
			        	alertDialog.setNegativeButton(android.R.string.no, null);
						final AlertDialog alert = alertDialog.create();
						// AlertDialog.Builder.setOnDismissListener() requires API level 17, so do it this way instead
						alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface arg0) {
								if(MyDebug.LOG)
									Log.d(TAG, "reset dialog dismissed");
								dialogs.remove(alert);
							}
						});
						alert.show();
						dialogs.add(alert);
                	}
                	return false;
                }
            });
        }
	}

	private void restartSaSiYa() {
		if(MyDebug.LOG)
			Log.d(TAG, "restartSaSiYa");
		MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
		main_activity.waitUntilImageQueueEmpty();
		// see http://stackoverflow.com/questions/2470870/force-application-to-restart-on-first-activity
		Intent i = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage( getActivity().getBaseContext().getPackageName() );
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
	}

	public static class SaveFolderChooserDialog extends FolderChooserDialog {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if(MyDebug.LOG)
				Log.d(TAG, "FolderChooserDialog dismissed");
			// n.b., fragments have to be static (as they might be inserted into a new Activity - see http://stackoverflow.com/questions/15571010/fragment-inner-class-should-be-static),
			// so we access the MainActivity via the fragment's getActivity().
			MainActivity main_activity = (MainActivity)this.getActivity();
			if( main_activity != null ) { // main_activity may be null if this is being closed via MainActivity.onNewIntent()
				String new_save_location = this.getChosenFolder();
				main_activity.updateSaveFolder(new_save_location);
			}
			super.onDismiss(dialog);
		}
	}

	private void readFromBundle(String [] values, String [] entries, String preference_key, String default_value, String preference_category_key) {
		if (MyDebug.LOG){
			Log.d(TAG, "readFromBundle");
		}
		if( values != null && values.length > 0 ) {
			if (MyDebug.LOG){
				Log.d(TAG, "values:");
				for(String value : values) {
					Log.d(TAG, value);
				}
			}
			ListPreference lp = (ListPreference)findPreference(preference_key);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			String value = sharedPreferences.getString(preference_key, default_value);
			if(MyDebug.LOG)
				Log.d(TAG, "    value: " + Arrays.toString(values));
			lp.setValue(value);
		}
		else {
			if(MyDebug.LOG)
				Log.d(TAG, "remove preference " + preference_key + " from category " + preference_category_key);
			Preference pref = findPreference(preference_key);
        	PreferenceGroup pg = (PreferenceGroup)this.findPreference(preference_category_key);
        	pg.removePreference(pref);
		}
	}
	
	public void onResume() {
		super.onResume();
		// prevent fragment being transparent
		// note, setting color here only seems to affect the "main" preference fragment screen, and not sub-screens
		// note, on Galaxy Nexus Android 4.3 this sets to black rather than the dark grey that the background theme should be (and what the sub-screens use); works okay on Nexus 7 Android 5
		// we used to use a light theme for the PreferenceFragment, but mixing themes in same activity seems to cause problems (e.g., for EditTextPreference colors)
		TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {  
			    android.R.attr.colorBackground
		});
		int backgroundColor = array.getColor(0, Color.BLACK);
		/*if (MyDebug.LOG){
			int r = (backgroundColor >> 16) & 0xFF;
			int g = (backgroundColor >> 8) & 0xFF;
			int b = (backgroundColor >> 0) & 0xFF;
			Log.d(TAG, "backgroundColor: " + r + " , " + g + " , " + b);
		}*/
		getView().setBackgroundColor(backgroundColor);
		array.recycle();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void onPause() {
		super.onPause();
	}

    @Override
    public void onDestroy() {
		if(MyDebug.LOG)
			Log.d(TAG, "onDestroy");
		super.onDestroy();

		// dismiss open dialogs - see comment for dialogs for why we do this
		for(AlertDialog dialog : dialogs) {
			if(MyDebug.LOG)
				Log.d(TAG, "dismiss dialog: " + dialog);
			dialog.dismiss();
		}
		// similarly dimissed any dialog fragments still opened
	    Fragment folder_fragment = getFragmentManager().findFragmentByTag("FOLDER_FRAGMENT");
    	if( folder_fragment != null ) {
	        DialogFragment dialogFragment = (DialogFragment)folder_fragment;
			if(MyDebug.LOG)
				Log.d(TAG, "dismiss dialogFragment: " + dialogFragment);
	        dialogFragment.dismiss();
    	}
	}

	/* So that manual changes to the checkbox/switch preferences, while the preferences are showing, show up;
	 * in particular, needed for preference_using_saf, when the user cancels the SAF dialog (see
	 * MainActivity.onActivityResult).
	 * Also programmatically sets summary (see setSummary).
	 */
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(MyDebug.LOG)
			Log.d(TAG, "onSharedPreferenceChanged");
	    Preference pref = findPreference(key);
	    if( pref instanceof TwoStatePreference ) {
	    	TwoStatePreference twoStatePref = (TwoStatePreference)pref;
	    	twoStatePref.setChecked(prefs.getBoolean(key, true));
	    }
	    else if( pref instanceof  ListPreference ) {
	    	ListPreference listPref = (ListPreference)pref;
	    	listPref.setValue(prefs.getString(key, ""));
		}
	    setSummary(key);
	}

	/** Programmatically sets summaries as required.
	 *  Remember to call setSummary() from the constructor for any keys we set, to initialise the
	 *  summary.
	 */
	private void setSummary(String key) {
		Preference pref = findPreference(key);
	    if( pref instanceof EditTextPreference ) {
	    	// %s only supported for ListPreference
			// we also display the usual summary if no preference value is set
	    	if( pref.getKey().equals("preference_exif_artist") ||
					pref.getKey().equals("preference_exif_copyright") ||
					pref.getKey().equals("preference_save_photo_prefix") ||
					pref.getKey().equals("preference_save_video_prefix") ||
					pref.getKey().equals("preference_textstamp")
					) {
	    		String default_value = "";
				if( pref.getKey().equals("preference_save_photo_prefix") )
					default_value = "IMG_";
				else if( pref.getKey().equals("preference_save_video_prefix") )
					default_value = "VID_";
				EditTextPreference editTextPref = (EditTextPreference)pref;
				if( editTextPref.getText().equals(default_value) ) {
					switch (pref.getKey()) {
						case "preference_exif_artist":
							pref.setSummary(R.string.preference_exif_artist_summary);
							break;
						case "preference_exif_copyright":
							pref.setSummary(R.string.preference_exif_copyright_summary);
							break;
						case "preference_save_photo_prefix":
							pref.setSummary(R.string.preference_save_photo_prefix_summary);
							break;
						case "preference_save_video_prefix":
							pref.setSummary(R.string.preference_save_video_prefix_summary);
							break;
						case "preference_textstamp":
							pref.setSummary(R.string.preference_textstamp_summary);
							break;
					}
				}
				else {
					// non-default value, so display the current value
					pref.setSummary(editTextPref.getText());
				}
			}
		}
	}
}
