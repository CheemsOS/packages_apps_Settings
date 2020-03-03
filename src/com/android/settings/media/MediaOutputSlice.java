/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.media.MediaDevice;

import java.util.Collection;

/**
 * Show the Media device that can be transfer the media.
 */
public class MediaOutputSlice implements CustomSliceable {

    private static final String TAG = "MediaOutputSlice";
    private static final String MEDIA_DEVICE_ID = "media_device_id";
    private static final int NON_SLIDER_VALUE = -1;

    public static final String MEDIA_PACKAGE_NAME = "media_package_name";

    private final Context mContext;

    private MediaDeviceUpdateWorker mWorker;

    public MediaOutputSlice(Context context) {
        mContext = context;
    }

    @VisibleForTesting
    void init(MediaDeviceUpdateWorker worker) {
        mWorker = worker;
    }

    @Override
    public Slice getSlice() {
        // Reload theme for switching dark mode on/off
        mContext.getTheme().applyStyle(R.style.Theme_Settings_Home, true /* force */);

        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED);

        if (!isVisible()) {
            Log.d(TAG, "getSlice() is not visible");
            return listBuilder.build();
        }

        final Collection<MediaDevice> devices = getMediaDevices();
        final MediaDeviceUpdateWorker worker = getWorker();
        final MediaDevice connectedDevice = worker.getCurrentConnectedMediaDevice();
        final boolean isTouched = worker.getIsTouched();
        // Fix the last top device when user press device to transfer.
        final MediaDevice topDevice = isTouched ? worker.getTopDevice() : connectedDevice;

        if (topDevice != null) {
            addRow(topDevice, connectedDevice, listBuilder);
            worker.setTopDevice(topDevice);
        }

        for (MediaDevice device : devices) {
            if (topDevice == null
                    || !TextUtils.equals(topDevice.getId(), device.getId())) {
                addRow(device, connectedDevice, listBuilder);
            }
        }

        return listBuilder.build();
    }

    private void addRow(MediaDevice device, MediaDevice connectedDevice, ListBuilder listBuilder) {
        if (connectedDevice != null && TextUtils.equals(device.getId(), connectedDevice.getId())) {
            listBuilder.addInputRange(getActiveDeviceHeaderRow(device));
        } else {
            listBuilder.addRow(getMediaDeviceRow(device));
        }
    }

    private ListBuilder.InputRangeBuilder getActiveDeviceHeaderRow(MediaDevice device) {
        final String title = device.getName();
        final IconCompat icon = getDeviceIconCompat(device);

        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, device.getId(), device.hashCode());
        final SliceAction primarySliceAction = SliceAction.createDeeplink(broadcastAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final ListBuilder.InputRangeBuilder builder = new ListBuilder.InputRangeBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primarySliceAction)
                .setInputAction(getSliderInputAction(device.hashCode(), device.getId()))
                .setMax(device.getMaxVolume())
                .setValue(device.getCurrentVolume());
        return builder;
    }

    private PendingIntent getSliderInputAction(int requestCode, String id) {
        final Intent intent = new Intent(getUri().toString())
                .setData(getUri())
                .putExtra(MEDIA_DEVICE_ID, id)
                .setClass(mContext, SliceBroadcastReceiver.class);

        return PendingIntent.getBroadcast(mContext, requestCode, intent, 0);
    }

    private IconCompat getDeviceIconCompat(MediaDevice device) {
        Drawable drawable = device.getIcon();
        if (drawable == null) {
            Log.d(TAG, "getDeviceIconCompat() device : " + device.getName() + ", drawable is null");
            // Use default Bluetooth device icon to handle getIcon() is null case.
            drawable = mContext.getDrawable(com.android.internal.R.drawable.ic_bt_headphones_a2dp);
        }

        return Utils.createIconWithDrawable(drawable);
    }

    private MediaDeviceUpdateWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    private Collection<MediaDevice> getMediaDevices() {
        final Collection<MediaDevice> devices = getWorker().getMediaDevices();
        return devices;
    }

    private ListBuilder.RowBuilder getMediaDeviceRow(MediaDevice device) {
        final String title = device.getName();
        final PendingIntent broadcastAction =
                getBroadcastIntent(mContext, device.getId(), device.hashCode());
        final IconCompat deviceIcon = getDeviceIconCompat(device);

        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(deviceIcon, ListBuilder.ICON_IMAGE)
                .setPrimaryAction(SliceAction.create(broadcastAction, deviceIcon,
                        ListBuilder.ICON_IMAGE, title))
                .setTitle(title)
                .setSubtitle(device.isConnected() ? null : device.getSummary());

        return rowBuilder;
    }

    private PendingIntent getBroadcastIntent(Context context, String id, int requestCode) {
        final Intent intent = new Intent(getUri().toString());
        intent.setClass(context, SliceBroadcastReceiver.class);
        intent.putExtra(MEDIA_DEVICE_ID, id);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final MediaDeviceUpdateWorker worker = getWorker();
        final String id = intent != null ? intent.getStringExtra(MEDIA_DEVICE_ID) : "";
        if (TextUtils.isEmpty(id)) {
            return;
        }
        final MediaDevice device = worker.getMediaDeviceById(id);
        if (device == null) {
            return;
        }
        final int newPosition = intent.getIntExtra(EXTRA_RANGE_VALUE, NON_SLIDER_VALUE);
        if (newPosition == NON_SLIDER_VALUE) {
            // Intent for device connection
            Log.d(TAG, "onNotifyChange() device name : " + device.getName());
            worker.setIsTouched(true);
            worker.connectDevice(device);
        } else {
            // Intent for volume adjustment
            worker.adjustVolume(device, newPosition);
        }
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return MediaDeviceUpdateWorker.class;
    }

    private boolean isVisible() {
        // To decide Slice's visibility.
        // Return true if
        // 1. AudioMode is not in on-going call
        // 2. worker is not null
        // 3. Bluetooth is enabled
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        return adapter.isEnabled()
                && !com.android.settingslib.Utils.isAudioModeOngoingCall(mContext)
                && getWorker() != null;
    }
}