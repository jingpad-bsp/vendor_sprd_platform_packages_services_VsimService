/*
 * Copyright (C) 2011, The Android Open Source Project
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
/*
 * Contributed by: zhanlei.feng@spreadtrum.com.
 */

package com.sprd.vsiminterface;

import com.sprd.vsiminterface.IVSIMCallback;
/**
 *  VSIM service interface.
 */
interface IVSIMInterface {
    int vsimInit(int phoneId, int restart, IVSIMCallback cb);
    int vsimSendData(int phoneId, in byte[] data, int data_len);
    int vsimExit(int phoneId);
    int vsimSetAuthid(int authid);
    int vsimQueryAuthid();
    int vsimSetVirtual(int phoneId, int mode);
    int vsimQueryVirtual(int phoneId);
    int vsimGetAuthCause(int phoneId);
    byte[] getAPDUFromRsim(int phoneId, in byte[] apdu_in);
    void setDefaultDataSubId(int subId);
    void setSimPowerStateForSlot(int slotId, boolean state);
    void attachAPN(int phoneId, String pdpType, String apn, String userName, String pwd, int authtype);
    String getSubscriberIdForSlotIdx(int soltId);
    int getSubId(int phoneId);
    int getVoiceRegState(int phoneId);
    int getDataRegState(int phoneId);
    String getNetworkOperator(int phoneId);
    int getVoiceNetworkType(int phoneId);
    int getDataNetworkType(int phoneId);
    int getSimState(int phoneId);
    void setDataEnabled(boolean enable);
    void setSimNeworkType(int phoneId, int type, boolean isPrimary);
    int getSimNeworkType(int phoneId);
    int getDefaultDataPhoneId();
    void setDefaultDataPhoneId(int phoneId);
    void restartRadio();
    int updatePlmn(int phoneId, int type, int action, String plmn, int act1, int act2, int act3);
    String[] queryPlmn(int phoneId, int type);
    int setImei(int phoneId, String imei);
    String getImei(int phoneId);
    boolean setPreferredNetworkType(int phoneId, int type);
    void powerRadio(int phoneId, boolean on);
    int vsimSetVirtualWithNv(int phoneId, int mode, int writeNV);
    String sendATCmd(int phoneId, String cmd);
}
