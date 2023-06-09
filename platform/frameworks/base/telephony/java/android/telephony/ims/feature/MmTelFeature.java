/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package android.telephony.ims.feature;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.TelecomManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsService;
import android.telephony.ims.RtpHeaderExtensionType;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import android.util.ArraySet;
import android.util.Log;

import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.internal.telephony.util.TelephonyUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Base implementation for Voice and SMS (IR-92) and Video (IR-94) IMS support.
 *
 * Any class wishing to use MmTelFeature should extend this class and implement all methods that the
 * service supports.
 */
public class MmTelFeature extends ImsFeature {

    private static final String LOG_TAG = "MmTelFeature";
    private Executor mExecutor;

    /**
     * @hide
     */
    @SystemApi
    public MmTelFeature() {
    }

    /**
     * Create a new MmTelFeature using the Executor specified for methods being called by the
     * framework.
     * @param executor The executor for the framework to use when executing the methods overridden
     * by the implementation of MmTelFeature.
     * @hide
     */
    @SystemApi
    public MmTelFeature(@NonNull Executor executor) {
        super();
        mExecutor = executor;
    }

    private final IImsMmTelFeature mImsMMTelBinder = new IImsMmTelFeature.Stub() {

        @Override
        public void setListener(IImsMmTelListener l) {
            executeMethodAsyncNoException(() -> MmTelFeature.this.setListener(l), "setListener");
        }

        @Override
        public int getFeatureState() throws RemoteException {
            return executeMethodAsyncForResult(() -> MmTelFeature.this.getFeatureState(),
                    "getFeatureState");
        }

        @Override
        public ImsCallProfile createCallProfile(int callSessionType, int callType)
                throws RemoteException {
            return executeMethodAsyncForResult(() -> MmTelFeature.this.createCallProfile(
                    callSessionType, callType), "createCallProfile");
        }

        @Override
        public void changeOfferedRtpHeaderExtensionTypes(List<RtpHeaderExtensionType> types)
                throws RemoteException {
            executeMethodAsync(() -> MmTelFeature.this.changeOfferedRtpHeaderExtensionTypes(
                    new ArraySet<>(types)), "changeOfferedRtpHeaderExtensionTypes");
        }

        @Override
        public IImsCallSession createCallSession(ImsCallProfile profile) throws RemoteException {
            AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsCallSession result = executeMethodAsyncForResult(() -> {
                try {
                    return createCallSessionInterface(profile);
                } catch (RemoteException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }, "createCallSession");

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }

            return result;
        }

        @Override
        public int shouldProcessCall(String[] numbers) {
            Integer result = executeMethodAsyncForResultNoException(() ->
                    MmTelFeature.this.shouldProcessCall(numbers), "shouldProcessCall");
            if (result != null) {
                return result.intValue();
            } else {
                return PROCESS_CALL_CSFB;
            }
        }

        @Override
        public IImsUt getUtInterface() throws RemoteException {
            AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsUt result = executeMethodAsyncForResult(() -> {
                try {
                    return MmTelFeature.this.getUtInterface();
                } catch (RemoteException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }, "getUtInterface");

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }

            return result;
        }

        @Override
        public IImsEcbm getEcbmInterface() throws RemoteException {
            AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsEcbm result = executeMethodAsyncForResult(() -> {
                try {
                    return MmTelFeature.this.getEcbmInterface();
                } catch (RemoteException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }, "getEcbmInterface");

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }

            return result;
        }

        @Override
        public void setUiTtyMode(int uiTtyMode, Message onCompleteMessage) throws RemoteException {
            executeMethodAsync(() -> MmTelFeature.this.setUiTtyMode(uiTtyMode, onCompleteMessage),
                    "setUiTtyMode");
        }

        @Override
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            AtomicReference<RemoteException> exceptionRef = new AtomicReference<>();
            IImsMultiEndpoint result = executeMethodAsyncForResult(() -> {
                try {
                    return MmTelFeature.this.getMultiEndpointInterface();
                } catch (RemoteException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }, "getMultiEndpointInterface");

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }

            return result;
        }

        @Override
        public int queryCapabilityStatus() {
            Integer result = executeMethodAsyncForResultNoException(() -> MmTelFeature.this
                    .queryCapabilityStatus().mCapabilities, "queryCapabilityStatus");

            if (result != null) {
                return result.intValue();
            } else {
                return 0;
            }
        }

        @Override
        public void addCapabilityCallback(IImsCapabilityCallback c) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .addCapabilityCallback(c), "addCapabilityCallback");
        }

        @Override
        public void removeCapabilityCallback(IImsCapabilityCallback c) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .removeCapabilityCallback(c), "removeCapabilityCallback");
        }

        @Override
        public void changeCapabilitiesConfiguration(CapabilityChangeRequest request,
                IImsCapabilityCallback c) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .requestChangeEnabledCapabilities(request, c),
                    "changeCapabilitiesConfiguration");
        }

        @Override
        public void queryCapabilityConfiguration(int capability, int radioTech,
                IImsCapabilityCallback c) {
            executeMethodAsyncNoException(() -> queryCapabilityConfigurationInternal(
                    capability, radioTech, c), "queryCapabilityConfiguration");
        }

        @Override
        public void setSmsListener(IImsSmsListener l) {
            executeMethodAsyncNoException(() -> MmTelFeature.this.setSmsListener(l),
                    "setSmsListener");
        }

        @Override
        public void sendSms(int token, int messageRef, String format, String smsc, boolean retry,
                byte[] pdu) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .sendSms(token, messageRef, format, smsc, retry, pdu), "sendSms");
        }

        @Override
        public void acknowledgeSms(int token, int messageRef, int result) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .acknowledgeSms(token, messageRef, result), "acknowledgeSms");
        }

        @Override
        public void acknowledgeSmsReport(int token, int messageRef, int result) {
            executeMethodAsyncNoException(() -> MmTelFeature.this
                    .acknowledgeSmsReport(token, messageRef, result), "acknowledgeSmsReport");
        }

        @Override
        public String getSmsFormat() {
            return executeMethodAsyncForResultNoException(() -> MmTelFeature.this
                    .getSmsFormat(), "getSmsFormat");
        }

        @Override
        public void onSmsReady() {
            executeMethodAsyncNoException(() -> MmTelFeature.this.onSmsReady(),
                    "onSmsReady");
        }

        // Call the methods with a clean calling identity on the executor and wait indefinitely for
        // the future to return.
        private void executeMethodAsync(Runnable r, String errorLogName) throws RemoteException {
            try {
                CompletableFuture.runAsync(
                        () -> TelephonyUtils.runWithCleanCallingIdentity(r), mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: "
                        + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private void executeMethodAsyncNoException(Runnable r, String errorLogName) {
            try {
                CompletableFuture.runAsync(
                        () -> TelephonyUtils.runWithCleanCallingIdentity(r), mExecutor).join();
            } catch (CancellationException | CompletionException e) {
                Log.w(LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: "
                        + e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResult(Supplier<T> r,
                String errorLogName) throws RemoteException {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    () -> TelephonyUtils.runWithCleanCallingIdentity(r), mExecutor);
            try {
                return future.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.w(LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: "
                        + e.getMessage());
                throw new RemoteException(e.getMessage());
            }
        }

        private <T> T executeMethodAsyncForResultNoException(Supplier<T> r,
                String errorLogName) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    () -> TelephonyUtils.runWithCleanCallingIdentity(r), mExecutor);
            try {
                return future.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.w(LOG_TAG, "MmTelFeature Binder - " + errorLogName + " exception: "
                        + e.getMessage());
                return null;
            }
        }
    };

    /**
     * Contains the capabilities defined and supported by a MmTelFeature in the form of a Bitmask.
     * The capabilities that are used in MmTelFeature are defined as
     * {@link MmTelCapabilities#CAPABILITY_TYPE_VOICE},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_VIDEO},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_UT},
     * {@link MmTelCapabilities#CAPABILITY_TYPE_SMS}, and
     * {@link MmTelCapabilities#CAPABILITY_TYPE_CALL_COMPOSER}.
     *
     * The capabilities of this MmTelFeature will be set by the framework.
     */
    public static class MmTelCapabilities extends Capabilities {

        /**
         * Create a new empty {@link MmTelCapabilities} instance.
         * @see #addCapabilities(int)
         * @see #removeCapabilities(int)
         * @hide
         */
        @SystemApi
        public MmTelCapabilities() {
            super();
        }

        /**@deprecated Use {@link MmTelCapabilities} to construct a new instance instead.
         * @hide
         */
        @Deprecated
        @SystemApi
        public MmTelCapabilities(Capabilities c) {
            mCapabilities = c.mCapabilities;
        }

        /**
         * Create a new {link @MmTelCapabilities} instance with the provided capabilities.
         * @param capabilities The capabilities that are supported for MmTel in the form of a
         *                     bitfield.
         * @hide
         */
        @SystemApi
        public MmTelCapabilities(@MmTelCapability int capabilities) {
            super(capabilities);
        }

        /** @hide */
        @IntDef(flag = true,
                value = {
                        CAPABILITY_TYPE_VOICE,
                        CAPABILITY_TYPE_VIDEO,
                        CAPABILITY_TYPE_UT,
                        CAPABILITY_TYPE_SMS,
                        CAPABILITY_TYPE_CALL_COMPOSER
                })
        @Retention(RetentionPolicy.SOURCE)
        public @interface MmTelCapability {}

        /**
         * Undefined capability type for initialization
         * This is used to check the upper range of MmTel capability
         * @hide
         */
        public static final int CAPABILITY_TYPE_NONE = 0;

        /**
         * This MmTelFeature supports Voice calling (IR.92)
         */
        public static final int CAPABILITY_TYPE_VOICE = 1 << 0;

        /**
         * This MmTelFeature supports Video (IR.94)
         */
        public static final int CAPABILITY_TYPE_VIDEO = 1 << 1;

        /**
         * This MmTelFeature supports XCAP over Ut for supplementary services. (IR.92)
         */
        public static final int CAPABILITY_TYPE_UT = 1 << 2;

        /**
         * This MmTelFeature supports SMS (IR.92)
         */
        public static final int CAPABILITY_TYPE_SMS = 1 << 3;

        /**
         * This MmTelFeature supports Call Composer (section 2.4 of RC.20)
         */
        public static final int CAPABILITY_TYPE_CALL_COMPOSER = 1 << 4;

        /**
         * This is used to check the upper range of MmTel capability
         * @hide
         */
        public static final int CAPABILITY_TYPE_MAX = CAPABILITY_TYPE_CALL_COMPOSER + 1;

        /**
         * @hide
         */
        @Override
        @SystemApi
        public final void addCapabilities(@MmTelCapability int capabilities) {
            super.addCapabilities(capabilities);
        }

        /**
         * @hide
         */
        @Override
        @SystemApi
        public final void removeCapabilities(@MmTelCapability int capability) {
            super.removeCapabilities(capability);
        }

        /**
         * @param capabilities a bitmask of one or more capabilities.
         *
         * @return true if all queried capabilities are true, otherwise false.
         */
        @Override
        public final boolean isCapable(@MmTelCapability int capabilities) {
            return super.isCapable(capabilities);
        }

        /**
         * @hide
         */
        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MmTel Capabilities - [");
            builder.append("Voice: ");
            builder.append(isCapable(CAPABILITY_TYPE_VOICE));
            builder.append(" Video: ");
            builder.append(isCapable(CAPABILITY_TYPE_VIDEO));
            builder.append(" UT: ");
            builder.append(isCapable(CAPABILITY_TYPE_UT));
            builder.append(" SMS: ");
            builder.append(isCapable(CAPABILITY_TYPE_SMS));
            builder.append(" CALL_COMPOSER: ");
            builder.append(isCapable(CAPABILITY_TYPE_CALL_COMPOSER));
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Listener that the framework implements for communication from the MmTelFeature.
     * @hide
     */
    public static class Listener extends IImsMmTelListener.Stub {

        /**
         * Called when the IMS provider receives an incoming call.
         * @param c The {@link ImsCallSession} associated with the new call.
         * @hide
         */
        @Override
        public void onIncomingCall(IImsCallSession c, Bundle extras) {

        }

        /**
         * Called when the IMS provider implicitly rejects an incoming call during setup.
         * @param callProfile An {@link ImsCallProfile} with the call details.
         * @param reason The {@link ImsReasonInfo} reason for call rejection.
         * @hide
         */
        @Override
        public void onRejectedCall(ImsCallProfile callProfile, ImsReasonInfo reason) {

        }

        /**
         * Updates the Listener when the voice message count for IMS has changed.
         * @param count an integer representing the new message count.
         * @hide
         */
        @Override
        public void onVoiceMessageCountUpdate(int count) {

        }
    }

    /**
     * To be returned by {@link #shouldProcessCall(String[])} when the ImsService should process the
     * outgoing call as IMS.
     * @hide
     */
    @SystemApi
    public static final int PROCESS_CALL_IMS = 0;
    /**
     * To be returned by {@link #shouldProcessCall(String[])} when the telephony framework should
     * not process the outgoing call as IMS and should instead use circuit switch.
     * @hide
     */
    @SystemApi
    public static final int PROCESS_CALL_CSFB = 1;

    /** @hide */
    @IntDef(flag = true,
            value = {
                    PROCESS_CALL_IMS,
                    PROCESS_CALL_CSFB
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProcessCallResult {}

    /**
     * If the flag is present and true, it indicates that the incoming call is for USSD.
     * <p>
     * This is an optional boolean flag.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_IS_USSD = "android.telephony.ims.feature.extra.IS_USSD";

    /**
     * If this flag is present and true, this call is marked as an unknown dialing call instead
     * of an incoming call. An example of such a call is a call that is originated by sending
     * commands (like AT commands) directly to the modem without Android involvement or dialing
     * calls appearing over IMS when the modem does a silent redial from circuit-switched to IMS in
     * certain situations.
     * <p>
     * This is an optional boolean flag.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_IS_UNKNOWN_CALL =
            "android.telephony.ims.feature.extra.IS_UNKNOWN_CALL";

    private IImsMmTelListener mListener;

    /**
     * @param listener A {@link Listener} used when the MmTelFeature receives an incoming call and
     *     notifies the framework.
     */
    private void setListener(IImsMmTelListener listener) {
        synchronized (mLock) {
            mListener = listener;
            if (mListener != null) {
                onFeatureReady();
            }
        }
    }

    /**
     * @return the listener associated with this MmTelFeature. May be null if it has not been set
     * by the framework yet.
     */
    private IImsMmTelListener getListener() {
        synchronized (mLock) {
            return mListener;
        }
    }

    /**
     * The current capability status that this MmTelFeature has defined is available. This
     * configuration will be used by the platform to figure out which capabilities are CURRENTLY
     * available to be used.
     *
     * Should be a subset of the capabilities that are enabled by the framework in
     * {@link #changeEnabledCapabilities}.
     * @return A copy of the current MmTelFeature capability status.
     * @hide
     */
    @Override
    @SystemApi
    public @NonNull final MmTelCapabilities queryCapabilityStatus() {
        return new MmTelCapabilities(super.queryCapabilityStatus());
    }

    /**
     * Notify the framework that the status of the Capabilities has changed. Even though the
     * MmTelFeature capability may be enabled by the framework, the status may be disabled due to
     * the feature being unavailable from the network.
     * @param c The current capability status of the MmTelFeature. If a capability is disabled, then
     * the status of that capability is disabled. This can happen if the network does not currently
     * support the capability that is enabled. A capability that is disabled by the framework (via
     * {@link #changeEnabledCapabilities}) should also show the status as disabled.
     * @hide
     */
    @SystemApi
    public final void notifyCapabilitiesStatusChanged(@NonNull MmTelCapabilities c) {
        if (c == null) {
            throw new IllegalArgumentException("MmTelCapabilities must be non-null!");
        }
        super.notifyCapabilitiesStatusChanged(c);
    }

    /**
     * Notify the framework of an incoming call.
     * @param c The {@link ImsCallSessionImplBase} of the new incoming call.
     * @param extras A bundle containing extra parameters related to the call. See
     * {@link #EXTRA_IS_UNKNOWN_CALL} and {@link #EXTRA_IS_USSD} above.
      * @hide
     */
    @SystemApi
    public final void notifyIncomingCall(@NonNull ImsCallSessionImplBase c,
            @NonNull Bundle extras) {
        if (c == null || extras == null) {
            throw new IllegalArgumentException("ImsCallSessionImplBase and Bundle can not be "
                    + "null.");
        }
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            c.setDefaultExecutor(MmTelFeature.this.mExecutor);
            listener.onIncomingCall(c.getServiceImpl(), extras);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notify the framework that a call has been implicitly rejected by this MmTelFeature
     * during call setup.
     * @param callProfile The {@link ImsCallProfile} IMS call profile with details.
     *        This can be null if no call information is available for the rejected call.
     * @param reason The {@link ImsReasonInfo} call rejection reason.
     * @hide
     */
    @SystemApi
    public final void notifyRejectedCall(@NonNull ImsCallProfile callProfile,
            @NonNull ImsReasonInfo reason) {
        if (callProfile == null || reason == null) {
            throw new IllegalArgumentException("ImsCallProfile and ImsReasonInfo must not be "
                    + "null.");
        }
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onRejectedCall(callProfile, reason);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @hide
     */
    public final void notifyIncomingCallSession(IImsCallSession c, Bundle extras) {
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onIncomingCall(c, extras);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notify the framework of a change in the Voice Message count.
     * @link count the new Voice Message count.
     * @hide
     */
    @SystemApi
    public final void notifyVoiceMessageCountUpdate(int count) {
        IImsMmTelListener listener = getListener();
        if (listener == null) {
            throw new IllegalStateException("Session is not available.");
        }
        try {
            listener.onVoiceMessageCountUpdate(count);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides the MmTelFeature with the ability to return the framework Capability Configuration
     * for a provided Capability. If the framework calls {@link #changeEnabledCapabilities} and
     * includes a capability A to enable or disable, this method should return the correct enabled
     * status for capability A.
     * @param capability The capability that we are querying the configuration for.
     * @return true if the capability is enabled, false otherwise.
     * @hide
     */
    @Override
    @SystemApi
    public boolean queryCapabilityConfiguration(@MmTelCapabilities.MmTelCapability int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int radioTech) {
        // Base implementation - Override to provide functionality
        return false;
    }

    /**
     * The MmTelFeature should override this method to handle the enabling/disabling of
     * MmTel Features, defined in {@link MmTelCapabilities.MmTelCapability}. The framework assumes
     * the {@link CapabilityChangeRequest} was processed successfully. If a subset of capabilities
     * could not be set to their new values,
     * {@link CapabilityCallbackProxy#onChangeCapabilityConfigurationError} must be called
     * individually for each capability whose processing resulted in an error.
     *
     * Enabling/Disabling a capability here indicates that the capability should be registered or
     * deregistered (depending on the capability change) and become available or unavailable to
     * the framework.
     * @hide
     */
    @Override
    @SystemApi
    public void changeEnabledCapabilities(@NonNull CapabilityChangeRequest request,
            @NonNull CapabilityCallbackProxy c) {
        // Base implementation, no-op
    }

    /**
     * Creates a {@link ImsCallProfile} from the service capabilities & IMS registration state.
     *
     * @param callSessionType a service type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#SERVICE_TYPE_NONE}
     *        {@link ImsCallProfile#SERVICE_TYPE_NORMAL}
     *        {@link ImsCallProfile#SERVICE_TYPE_EMERGENCY}
     * @param callType a call type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#CALL_TYPE_VOICE}
     *        {@link ImsCallProfile#CALL_TYPE_VT}
     *        {@link ImsCallProfile#CALL_TYPE_VT_TX}
     *        {@link ImsCallProfile#CALL_TYPE_VT_RX}
     *        {@link ImsCallProfile#CALL_TYPE_VT_NODIR}
     *        {@link ImsCallProfile#CALL_TYPE_VS}
     *        {@link ImsCallProfile#CALL_TYPE_VS_TX}
     *        {@link ImsCallProfile#CALL_TYPE_VS_RX}
     * @return a {@link ImsCallProfile} object
     * @hide
     */
    @SystemApi
    public @Nullable ImsCallProfile createCallProfile(int callSessionType, int callType) {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * Called by the framework to report a change to the RTP header extension types which should be
     * offered during SDP negotiation (see RFC8285 for more information).
     * <p>
     * The {@link ImsService} should report the RTP header extensions which were accepted during
     * SDP negotiation using {@link ImsCallProfile#setAcceptedRtpHeaderExtensionTypes(Set)}.
     *
     * @param extensionTypes The RTP header extensions the framework wishes to offer during
     *                       outgoing and incoming call setup.  An empty list indicates that there
     *                       are no framework defined RTP header extension types to offer.
     * @hide
     */
    @SystemApi
    public void changeOfferedRtpHeaderExtensionTypes(
            @NonNull Set<RtpHeaderExtensionType> extensionTypes) {
        // Base implementation - should be overridden if RTP header extension handling is supported.
    }

    /**
     * @hide
     */
    public IImsCallSession createCallSessionInterface(ImsCallProfile profile)
            throws RemoteException {
        ImsCallSessionImplBase s = MmTelFeature.this.createCallSession(profile);
        if (s != null) {
            s.setDefaultExecutor(mExecutor);
            return s.getServiceImpl();
        } else {
            return null;
        }
    }

    /**
     * Creates an {@link ImsCallSession} with the specified call profile.
     * Use other methods, if applicable, instead of interacting with
     * {@link ImsCallSession} directly.
     *
     * @param profile a call profile to make the call
     * @hide
     */
    @SystemApi
    public @Nullable ImsCallSessionImplBase createCallSession(@NonNull ImsCallProfile profile) {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * Called by the framework to determine if the outgoing call, designated by the outgoing
     * {@link String}s, should be processed as an IMS call or CSFB call. If this method's
     * functionality is not overridden, the platform will process every call as IMS as long as the
     * MmTelFeature reports that the {@link MmTelCapabilities#CAPABILITY_TYPE_VOICE} capability is
     * available.
     * @param numbers An array of {@link String}s that will be used for placing the call. There can
     *         be multiple {@link String}s listed in the case when we want to place an outgoing
     *         call as a conference.
     * @return a {@link ProcessCallResult} to the framework, which will be used to determine if the
     *        call will be placed over IMS or via CSFB.
     * @hide
     */
    @SystemApi
    public @ProcessCallResult int shouldProcessCall(@NonNull String[] numbers) {
        return PROCESS_CALL_IMS;
    }

    /**
     *
     * @hide
     */
    protected IImsUt getUtInterface() throws RemoteException {
        ImsUtImplBase utImpl = getUt();
        if (utImpl != null) {
            utImpl.setDefaultExecutor(mExecutor);
            return utImpl.getInterface();
        } else {
            return null;
        }
    }

    /**
     * @hide
     */
    protected IImsEcbm getEcbmInterface() throws RemoteException {
        ImsEcbmImplBase ecbmImpl = getEcbm();
        if (ecbmImpl != null) {
            ecbmImpl.setDefaultExecutor(mExecutor);
            return ecbmImpl.getImsEcbm();
        } else {
            return null;
        }
    }

    /**
     * @hide
     */
    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        ImsMultiEndpointImplBase multiendpointImpl = getMultiEndpoint();
        if (multiendpointImpl != null) {
            multiendpointImpl.setDefaultExecutor(mExecutor);
            return multiendpointImpl.getIImsMultiEndpoint();
        } else {
            return null;
        }
    }

    /**
     * @return The {@link ImsUtImplBase} Ut interface implementation for the supplementary service
     * configuration.
     * @hide
     */
    @SystemApi
    public @NonNull ImsUtImplBase getUt() {
        // Base Implementation - Should be overridden
        return new ImsUtImplBase();
    }

    /**
     * @return The {@link ImsEcbmImplBase} Emergency call-back mode interface for emergency VoLTE
     * calls that support it.
     * @hide
     */
    @SystemApi
    public @NonNull ImsEcbmImplBase getEcbm() {
        // Base Implementation - Should be overridden
        return new ImsEcbmImplBase();
    }

    /**
     * @return The {@link ImsMultiEndpointImplBase} implementation for implementing Dialog event
     * package processing for multi-endpoint.
     * @hide
     */
    @SystemApi
    public @NonNull ImsMultiEndpointImplBase getMultiEndpoint() {
        // Base Implementation - Should be overridden
        return new ImsMultiEndpointImplBase();
    }

    /**
     * Sets the current UI TTY mode for the MmTelFeature.
     * @param mode An integer containing the new UI TTY Mode, can consist of
     *         {@link TelecomManager#TTY_MODE_OFF},
     *         {@link TelecomManager#TTY_MODE_FULL},
     *         {@link TelecomManager#TTY_MODE_HCO},
     *         {@link TelecomManager#TTY_MODE_VCO}
     * @param onCompleteMessage If non-null, this MmTelFeature should call this {@link Message} when
     *         the operation is complete by using the associated {@link android.os.Messenger} in
     *         {@link Message#replyTo}. For example:
     * {@code
     *     // Set UI TTY Mode and other operations...
     *     try {
     *         // Notify framework that the mode was changed.
     *         Messenger uiMessenger = onCompleteMessage.replyTo;
     *         uiMessenger.send(onCompleteMessage);
     *     } catch (RemoteException e) {
     *         // Remote side is dead
     *     }
     * }
     * @hide
     */
    @SystemApi
    public void setUiTtyMode(int mode, @Nullable Message onCompleteMessage) {
        // Base Implementation - Should be overridden
    }

    private void setSmsListener(IImsSmsListener listener) {
        getSmsImplementation().registerSmsListener(listener);
    }

    private void sendSms(int token, int messageRef, String format, String smsc, boolean isRetry,
            byte[] pdu) {
        getSmsImplementation().sendSms(token, messageRef, format, smsc, isRetry, pdu);
    }

    private void acknowledgeSms(int token, int messageRef,
            @ImsSmsImplBase.DeliverStatusResult int result) {
        getSmsImplementation().acknowledgeSms(token, messageRef, result);
    }

    private void acknowledgeSmsReport(int token, int messageRef,
            @ImsSmsImplBase.StatusReportResult int result) {
        getSmsImplementation().acknowledgeSmsReport(token, messageRef, result);
    }

    private void onSmsReady() {
        getSmsImplementation().onReady();
    }

    /**
     * Must be overridden by IMS Provider to be able to support SMS over IMS. Otherwise a default
     * non-functional implementation is returned.
     *
     * @return an instance of {@link ImsSmsImplBase} which should be implemented by the IMS
     * Provider.
     * @hide
     */
    @SystemApi
    public @NonNull ImsSmsImplBase getSmsImplementation() {
        return new ImsSmsImplBase();
    }

    private String getSmsFormat() {
        return getSmsImplementation().getSmsFormat();
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    @SystemApi
    public void onFeatureRemoved() {
        // Base Implementation - Should be overridden
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    @SystemApi
    public void onFeatureReady() {
        // Base Implementation - Should be overridden
    }

    /**
     * @hide
     */
    @Override
    public final IImsMmTelFeature getBinder() {
        return mImsMMTelBinder;
    }

    /**
     * Set default Executor from ImsService.
     * @param executor The default executor for the framework to use when executing the methods
     * overridden by the implementation of MmTelFeature.
     * @hide
     */
    public final void setDefaultExecutor(@NonNull Executor executor) {
        if (mExecutor == null) {
            mExecutor = executor;
        }
    }
}
