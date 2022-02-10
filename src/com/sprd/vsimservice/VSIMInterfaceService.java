/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.sprd.vsimservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.Arrays;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManagerEx;
import com.android.sprd.telephony.RadioInteractor;
import com.sprd.vsiminterface.IVSIMCallback;
import com.sprd.vsiminterface.IVSIMInterface;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.RILConstants;
import android.text.TextUtils;
import android.os.SystemProperties;

public class VSIMInterfaceService extends Service {
    static {
        System.loadLibrary("vsim_jni");
    }

    private static final String LOG_TAG = "VSIMInterfaceService";
    private static final String WORK_MODE = "persist.vendor.radio.modem.workmode";
    private static final String AT_IMEI = "AT+SPYSIMEI=";
    private static final String AT_READ_IMEI = "AT+CGSN";
    private int mPhoneCount;
    private  IVSIMCallback[] callbacks;
    private Context mContext = null;
    private TelephonyManagerEx mTmx = null;
    private TelephonyManager mTm = null;
    private SubscriptionManager mSm = null;

    public VSIMInterfaceService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate(), build date: 20180521.1108");
        mContext = getBaseContext();
        mTmx = TelephonyManagerEx.from(mContext);
        mTm = TelephonyManager.from(mContext);
        mSm = SubscriptionManager.from(mContext);
        mPhoneCount = mTm.getPhoneCount();
        callbacks = new IVSIMCallback[mPhoneCount];
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
    }

    //when bind service, the action of intent must be "com.sprd.vsiminterface.BIND_SERVICE"
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind()");
        //if ("com.sprd.vsiminterface.IVSIMInterface".equals(intent.getAction())) {
            return new VSIMServiceBinder();
        //}
        //return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind()");
        return true;
    }

    public byte[] serviceCallback(int slot, byte[] apdu, int length) {
        Log.d(LOG_TAG, "callback upload APDU");

        try {
             byte[] res = callbacks[slot].uploadAPDU(slot, apdu, length);
             Log.d(LOG_TAG, "callback return" + Arrays.toString(res));
             return res;
        } catch (RemoteException e) {
            Log.d(LOG_TAG, "callback remote exception happened!");
        }

        //setAPDUTONative(res);
        return null;
    }

    /**
     * The Binder interface implementation.
     */
    private class VSIMServiceBinder extends IVSIMInterface.Stub {

        //TODO: replace the funcs
        @Override
        public int vsimInit(int phoneId, int restart, IVSIMCallback cb) {
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                return -1;
            }
            Log.d(LOG_TAG, "call native: vsimInit");
            callbacks[phoneId] = cb;
            return vsimInitNative(phoneId, restart);
        }

        @Override
        public int vsimSendData(int phoneId, byte[] data, int data_len) {
            Log.d(LOG_TAG, "call native: vsimSendData");
            return vsimSendDataNative(phoneId, data, data_len);
        }

        @Override
        public int vsimExit(int phoneId) {
            Log.d(LOG_TAG, "call native: vsimExit");
            return vsimExitNative(phoneId);
        }

        @Override
        public int vsimSetAuthid(int authid) {
            Log.d(LOG_TAG, "call native: vsimSetAuthid");
            return vsimSetAuthidNative(authid);
        }

        @Override
        public int vsimQueryAuthid() {
            Log.d(LOG_TAG, "call native: vsimQueryAuthid");
            return vsimQueryAuthidNative();
        }

        @Override
        public int vsimSetVirtual(int phoneId, int mode) {
            Log.d(LOG_TAG, "call native: vsimSetVirtual");
            return vsimSetVirtualNative(phoneId, mode);
        }

        @Override
        public int vsimQueryVirtual(int phoneId) {
            Log.d(LOG_TAG, "call native: vsimQueryVirtual");
            return vsimQueryVirtualNative(phoneId);
        }

        @Override
        public int vsimGetAuthCause(int phoneId) {
            Log.d(LOG_TAG, "call native: vsimGetAuthCause");
            return vsimGetAuthCauseNative(phoneId);
        }

        @Override
        public byte[] getAPDUFromRsim(int phoneId, byte[] apdu_in) {
            Log.d(LOG_TAG, "call native: getAPDUFromRsim");
            return getAPDUFromRsimNative(phoneId, apdu_in);
        }

        @Override
        public void setDefaultDataSubId(int subId) {
            Log.d(LOG_TAG, "setDefaultDataSubId: subId = " + subId);
            mSm.setDefaultDataSubId(subId);
        }

        //shut down real sim card
/*
        @Override
        public boolean shutDownRsim(int slotId) {
            Log.d(LOG_TAG, "shutDownRsim: slotId = " + slotId);
            RadioInteractor radioInteractor = new RadioInteractor(mContext);
            return radioInteractor.requestShutdown(slotId);
        }
*/
        @Override
        public void setSimPowerStateForSlot(int slotId, boolean state) {
            Log.d(LOG_TAG, "setSimPowerStateforSlot: slotId = " + slotId + " state = " + state);
            RadioInteractor radioInteractor = new RadioInteractor(mContext);

//            if(state) {
            radioInteractor.setSimPowerReal(slotId, state);
//            } else {
//                radioInteractor.requestShutdown(slotId);
//            }
        }

        /*
         * AT+CGDCONT=1,"IPV4V6","APN","",0,0
         * AT+CGPCO=0,"user","password",1,authtype
         *
         * if no authtype, it's value need to be set as default value 3
         */
        @Override
        public void attachAPN(int phoneId, String pdpType, String apn, String userName, String pwd, int authtype) {
            if (pdpType == null || apn == null || userName == null || pwd == null) {
                Log.d(LOG_TAG, "attach APN: some params is null!");
                return;
            }

            String cmd1 = "AT+CGDCONT=1," + "\"" + pdpType + "\",\"" + apn + "\",\"\",0,0";
            String cmd2 = "AT+CGPCO=0," + "\"" + userName + "\",\"" + pwd + "\",1," + authtype;

            Log.d(LOG_TAG, "attach APN: phoneId = " + phoneId);
            Log.d(LOG_TAG, "cmd1 =: " + cmd1);
            Log.d(LOG_TAG, "cmd2 =: " + cmd2);

            sendATCmdNative(phoneId, cmd1);
            sendATCmdNative(phoneId, cmd2);
        }

        @Override
        public String getSubscriberIdForSlotIdx(int slotId) {
            Log.d(LOG_TAG, "getSubscriberIdForSlotIdx: slotId = " + slotId);
            int subId =  SubscriptionManager.getSubId(slotId)[0];
            if (subId < 0) {
                return null;
            }
            return mTm.getSubscriberId(subId);
        }

        @Override
        public int getSubId(int phoneId){
            int[] subIds = SubscriptionManager.getSubId(phoneId);
            if (subIds == null) {
                return -1;
            }
            return subIds[0];
        }

        @Override
        public int getVoiceRegState(int phoneId){
            int subId = getSubId(phoneId);
            Log.d(LOG_TAG, "getVoiceRegState: phoneId = " + phoneId + ", subid = " + subId);
            if (subId < 0) {
                return -1;
            }
            ServiceState state = mTm.getServiceStateForSubscriber(subId);
            if (state != null) {
                return state.getVoiceRegState();
            }
            return -1;
        }

        @Override
        public int getDataRegState(int phoneId){
            int subId = getSubId(phoneId);
            Log.d(LOG_TAG, "getDataRegState: phoneId = " + phoneId + ", subid = " + subId);
            if (subId < 0) {
                return -1;
            }
            ServiceState state = mTm.getServiceStateForSubscriber(subId);
            if (state != null) {
                return state.getDataRegState();
            }
            return -1;
        }

        @Override
        public String getNetworkOperator(int phoneId){
            Log.d(LOG_TAG, "getNetworkOperator: phoneId = " + phoneId);
            return mTm.getNetworkOperatorForPhone(phoneId);
        }

        @Override
        public int getDataNetworkType(int phoneId){
            int subId = getSubId(phoneId);
            Log.d(LOG_TAG, "getDataNetworkType: phoneId = " + phoneId + ", subid = " + subId);
            if (subId < 0) {
                return -1;
            }
            return mTm.getDataNetworkType(subId);
        }

        @Override
        public int getVoiceNetworkType(int phoneId){
            int subId = getSubId(phoneId);
            Log.d(LOG_TAG, "getVoiceNetworkType: phoneId = " + phoneId + ", subid = " + subId);
            if (subId < 0) {
                return -1;
            }
            return mTm.getVoiceNetworkType(subId);
        }

        @Override
        public int getSimState(int phoneId){
            Log.d(LOG_TAG, "getSimState: phoneId = " + phoneId);
            return mTm.getSimState(phoneId);
        }

        @Override
        public void setDataEnabled(boolean enable){
            Log.d(LOG_TAG, "setDataEnabled: enable = " + enable);
            mTm.setDataEnabled(enable);
        }

        @Override
        public void setSimNeworkType(int phoneId, int type, boolean isPrimary){
            Log.d(LOG_TAG, "setSimNeworkType: phoneId = " + phoneId + ", type = " + type
                 + ", isPrimary =  " + isPrimary);
            int mode = 0;
            switch (type) {
                case RILConstants.NETWORK_MODE_WCDMA_PREF:
                    if (isPrimary) {
                        mode = 22;
                    } else {
                        mode = 14;
                    }
                    break;
                case RILConstants.NETWORK_MODE_GSM_ONLY:
                    mode = 10;
                    break;
                case RILConstants.NETWORK_MODE_WCDMA_ONLY:
                    if (isPrimary) {
                        mode = 18;
                    } else {
                        mode = 11;
                    }
                    break;
                case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                    mode = 9;
                    break;
                case RILConstants.NETWORK_MODE_LTE_ONLY:
                    mode = 3;
                    break;
                case RILConstants.NETWORK_MODE_LTE_WCDMA:
                    mode = 24;
                    break;
            }
            String configMode = SystemProperties.get(WORK_MODE, "");
            Log.d(LOG_TAG, "configMode = " + configMode + ", mode = " + mode);

            if (!TextUtils.isEmpty(configMode) && configMode.contains(",")) {
                String[] modes = configMode.split(",");
                if (modes.length == 2) {
                    if (phoneId == 0) {
                        configMode = mode + "," + modes[1];
                    } else if (phoneId == 1){
                        configMode = modes[0] + "," + mode;
                    }
                }
                Log.d(LOG_TAG, "end configMode = " + configMode);
                SystemProperties.set(WORK_MODE, configMode);
            }
        }

        @Override
        public int getSimNeworkType(int phoneId) {
            Log.d(LOG_TAG,"getSimNeworkType phoneId = " + phoneId);
            int type = -1;
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                return type;
            }

            String configMode = SystemProperties.get(WORK_MODE, "");
            Log.d(LOG_TAG, "configedMode = " + configMode);
            int mode = 0;
            if (!TextUtils.isEmpty(configMode) && configMode.contains(",")) {
                String[] modes = configMode.split(",");
                if (modes.length == 2) {
                    mode = Integer.valueOf(modes[phoneId]);
                }
                switch (mode) {
                    case 3:
                        type = RILConstants.NETWORK_MODE_LTE_ONLY;
                        break;
                    case 24:
                        type = RILConstants.NETWORK_MODE_LTE_WCDMA;
                        break;
                    case 9:
                        type = RILConstants.NETWORK_MODE_LTE_GSM_WCDMA;
                        break;
                    case 11:
                    case 18:
                        type = RILConstants.NETWORK_MODE_WCDMA_ONLY;
                        break;
                    case 10:
                        type = RILConstants.NETWORK_MODE_GSM_ONLY;
                        break;
                    case 22:
                    case 14:
                        type = RILConstants.NETWORK_MODE_WCDMA_PREF;
                        break;
                }
            }
            return type;
        }

        @Override
        public int getDefaultDataPhoneId(){
            Log.d(LOG_TAG, "getDefaultDataPhoneId");
            return mSm.getDefaultDataPhoneId();
        }

        @Override
        public void setDefaultDataPhoneId(int phoneId){
            int subId = getSubId(phoneId);
            Log.d(LOG_TAG, "setDefaultDataPhoneId: phoneId = " + phoneId + ", subid = " + subId);
            if (subId < 0) {
                return;
            }
            mSm.setDefaultDataSubId(subId);
        }

        @Override
        public void restartRadio() {
            //Restart all radio to change test mode for app
            Log.d(LOG_TAG, "restartRadio");
            int phoneCount = mTm.getPhoneCount();
            mTmx.setRadioPower(phoneCount, false);
            mTmx.setRadioPower(phoneCount, true);
        }
        /**
         * insert or delete OPLMN/FPLMN
         * @param phoneId
         * @param type: 0--FPLMN, 1--OPLMN
         * @param action:0--delete, 1--insert, 2--delete all
         * @param plmn
         * @param act1:for OPLMN
         * @param act2:for OPLMN
         * @param act3:for OPLMN
         * @return success 1, error -1
         */
        @Override
        public int updatePlmn(int phoneId, int type, int action, String plmn,
                              int act1, int act2, int act3) {
            Log.d(LOG_TAG, "updatePlmn: phoneId = " + phoneId);
            RadioInteractor radioInteractor = new RadioInteractor(mContext);
            return radioInteractor.updatePlmn(phoneId, type, action, plmn, act1, act2, act3);
        }

        /**
         *
         * @param phoneId
         * @param type: 0--FPLMN, 1--OPLMN
         * @return String array of OPLMN:
         *    index1, format, oper1, GSM_AcT1, TD_AcT1, GSM_Compact_AcT1, UTRAN_AcT1
         */
        @Override
        public String[] queryPlmn(int phoneId, int type) {
            Log.d(LOG_TAG, "queryPlmn: phoneId = " + phoneId);
            String[] result = null;
            RadioInteractor radioInteractor = new RadioInteractor(mContext);
            String str = radioInteractor.queryPlmn(phoneId, type);
            if (!TextUtils.isEmpty(str)) {
                result = str.split(";");
            }
            return result;
        }

        @Override
        public int setImei(int phoneId, String imei) {
            Log.d(LOG_TAG, "setXXXX phoneId = " + phoneId);
            String writeImeiAT = null;
            if (TextUtils.isEmpty(imei)) {
                Log.d(LOG_TAG, "xx is empty");
                writeImeiAT = AT_IMEI + "0";
            }
            //only receive imei length 15.
            if (imei.length() == 15) {
                Log.d(LOG_TAG, "xx length");
                char[] imeiArr = imei.toCharArray();
                char[] newImeiArr = new char[16];
                newImeiArr[0] = imeiArr[0];
                newImeiArr[1] = '9';
                for (int i = 1; i < imeiArr.length; i++) {
                    if (i % 2 == 1) {
                        newImeiArr[i+2] = imeiArr[i];
                    } else {
                        newImeiArr[i] = imeiArr[i];
                    }
                }
                writeImeiAT = AT_IMEI + "1,\"" + String.valueOf(newImeiArr) + "\"";
            }
            if (!TextUtils.isEmpty(writeImeiAT)) {
                String result = sendATCmdNative(phoneId, writeImeiAT);
                if (result.contains("OK")) {
                    return 1;
                }
            }
            return -1;
        }

        @Override
        public String getImei(int phoneId) {
            String imei = "";
            Log.d(LOG_TAG, "getXXXX phoneId = " + phoneId);
            String result = sendATCmdNative(phoneId, AT_READ_IMEI);
            if (result.contains("OK")) {
                String[] str = result.split("\n");
                if (str.length > 0) {
                    imei = str[0];
                }
            }
            return imei;
        }

        @Override
        public boolean setPreferredNetworkType(int phoneId, int type) {
            Log.d(LOG_TAG, "setPreferredNetworkType: phoneId = " + phoneId + ", type = " + type);
            int subId = getSubId(phoneId);
            if (subId < 0) {
                return false;
            }
            return mTm.setPreferredNetworkType(subId, type);
        }
        @Override
        public void powerRadio(int phoneId, boolean on){
            Log.d(LOG_TAG, "powerRadio: phoneId = " + phoneId + ", on = " + on);
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                mTmx.setRadioPower(phoneId, on);
            } else {
                Log.d(LOG_TAG, "invalid phoneId");
            }
        }

        @Override
        public int vsimSetVirtualWithNv(int phoneId, int mode, int writeNV) {
            Log.d(LOG_TAG, "call native: vsimSetVirtualWithNv");
            return vsimSetVirtualWithNVNative(phoneId, mode, writeNV);
        }

        @Override
        public String sendATCmd(int phoneId, String cmd) {
            Log.d(LOG_TAG, "call native: sendATCmdNative");
            return sendATCmdNative(phoneId, cmd);
        }
    }

    //int vsim_init(int phoneId, VSIM_COMMAND pfn, int restart)
    public native int vsimInitNative(int phoneId, int restart);

    //int vsim_send_data(int phoneId, u8* data, u16 data_len)
    public native int vsimSendDataNative(int phoneId, byte[] data, int data_len);

    //int vsim_exit(int phoneId)
    public native int vsimExitNative(int phoneId);

    //int vsim_set_authid(int authid)
    public native int vsimSetAuthidNative(int authid);

    //int vsim_query_authid()
    public native int vsimQueryAuthidNative();

    //int vsim_set_virtual(int phoneId, int mode)
    public native int vsimSetVirtualNative(int phoneId, int mode);

    //int vsim_set_virtual(int phoneId, int mode)
    public native int vsimSetVirtualWithNVNative(int phoneId, int mode, int writeNV);

    //int vsim_query_virtual(int phoneId)
    public native int vsimQueryVirtualNative(int phoneId);

    //int vsim_get_auth_cause(int phoneId);
    public native int vsimGetAuthCauseNative(int phoneId);

    public native byte[] getAPDUFromRsimNative(int phoneId, byte[] apdu_in);

    /*
     * if failed, return "ERROR"
     */
    public native String sendATCmdNative(int phoneId, String cmd);
}
