/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.app;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.titanium.proxy.RProxy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import java.util.ArrayList;

@Kroll.module(parentModule=AppModule.class)
public class AndroidModule extends KrollModule
{
	protected RProxy r;
	private static final String TAG = "App.AndroidModule";
	private int appVersionCode = -1;
	private String appVersionName;

	public AndroidModule()
	{
		super();
	}

	public AndroidModule(TiContext context)
	{
		this();
	}

	@Kroll.getProperty(name="R")
	public RProxy getR()
	{
		if (r == null) {
			r = new RProxy(RProxy.RESOURCE_TYPE_APPLICATION);
		}
		return r;
	}

	// this shouldn't be called from anything other than the runtime thread
	@Kroll.method
	public ActivityProxy getTopActivity()
	{
		if (KrollRuntime.getActivityRefCount() == 0) {
			// No activity to wait for. This can be the case if, for example,
			// the Application is being started for a Service, not an Activity.
			return null;
		}
		TiApplication tiApp = TiApplication.getInstance();
		Activity activity = tiApp.getCurrentActivity();
		if (activity == null || !(activity instanceof TiBaseActivity)) {
			try {
				tiApp.rootActivityLatch.await();
				activity = tiApp.getRootActivity();
			} catch (InterruptedException e) {
				Log.e(TAG, "Interrupted awaiting rootActivityLatch");
			}
		}

		if (activity instanceof TiBaseActivity) {
			return ((TiBaseActivity)activity).getActivityProxy();
		} else {
			return null;
		}
	}
	
	@Kroll.getProperty
	@Kroll.method
	public int getAppVersionCode()
	{
		if (appVersionCode == -1) {
			initializeVersionValues();
		}
		return appVersionCode;
	}

	@Kroll.getProperty @Kroll.method
	public IntentProxy getLaunchIntent()
	{
		TiApplication app = TiApplication.getInstance();
		if (app != null) {
			TiBaseActivity rootActivity = app.getRootActivity();
			if (rootActivity != null) {
				Intent intent = rootActivity.getIntent();
				if (intent != null) {
					return new IntentProxy(intent);
				}
			}
		}
		return null;
	}

	@Kroll.getProperty
	@Kroll.method
	public String getAppVersionName()
	{
		if (appVersionName == null) {
			initializeVersionValues();
		}
		return appVersionName;
	}

	private void initializeVersionValues()
	{
		PackageInfo pInfo;
		try {
			pInfo = TiApplication.getInstance().getPackageManager()
				.getPackageInfo(TiApplication.getInstance().getPackageName(), 0);
			appVersionCode = pInfo.versionCode;
			appVersionName = pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to get package info", e);
		}
	}

	@Override
	public String getApiName()
	{
		return "Ti.App.Android";
	}
	
	@Kroll.method
	public void requestPermissions(String[] permissionArgs, @Kroll.argument(optional=true)KrollFunction permissionCallback)
	{
		ArrayList<String> mPermissions = new ArrayList<String>();
         
        Activity currentActivity = TiApplication.getInstance().getCurrentActivity();
		if (TiBaseActivity.generalCallbackContext == null) {
			TiBaseActivity.generalCallbackContext = getKrollObject();
		}

		for(int i=0 ; i < permissionArgs.length ; i++){
			String permission = permissionArgs[i];
			if (ContextCompat.checkSelfPermission(currentActivity, permission) != PackageManager.PERMISSION_GRANTED) {
				mPermissions.add(permission);
			}
		}
		if(mPermissions.size()==0){
			if(permissionCallback!=null){
				KrollDict response = new KrollDict();
				response.putCodeAndMessage(0, null);
				permissionCallback.callAsync(TiBaseActivity.generalCallbackContext, response);
			}
			return;
		}

		String[] permissions = mPermissions.toArray(new String[mPermissions.size()]);
		TiBaseActivity.generalPermissionCallback = permissionCallback;
		
		currentActivity.requestPermissions(permissions, TiC.PERMISSION_CODE_GENERAL);
	}	
}

