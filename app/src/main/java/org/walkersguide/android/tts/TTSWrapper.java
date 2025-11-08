package org.walkersguide.android.tts;

import androidx.annotation.RequiresApi;
import org.walkersguide.android.R;
import android.os.Looper;
import android.os.Handler;
import org.walkersguide.android.util.Helper;


import android.content.Context;

import android.os.Build;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.view.accessibility.AccessibilityManager;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.media.AudioAttributes;
import android.accessibilityservice.AccessibilityServiceInfo;
import timber.log.Timber;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;
import android.media.AudioManager;
import android.annotation.SuppressLint;
import android.media.AudioFocusRequest;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collections;


public class TTSWrapper extends UtteranceProgressListener {
    private static final long SILENCE_DELAY = 400;
    private static final String SILENCE_UTTERANCE_ID = "utterance_id_silence";

    private static TTSWrapper managerInstance;
    private AccessibilityManager accessibilityManager;
    private AudioManager mAudioManager;
    private TextToSpeech tts;
    private LinkedList<MessageQueueItem> messageQueue;
    private MessageQueueItem lastItem;

    public static TTSWrapper getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized TTSWrapper getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new TTSWrapper();
        }
        return managerInstance;
    }

    private TTSWrapper() {
        accessibilityManager = (AccessibilityManager) GlobalInstance.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        mAudioManager = (AudioManager) GlobalInstance.getContext().getSystemService(Context.AUDIO_SERVICE);
        appendToLog("\nopen session\n");

        tts = new TextToSpeech(GlobalInstance.getContext(), new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Helper.getAppLocale());
                    tts.setOnUtteranceProgressListener(TTSWrapper.this);
                    //startTestMessages();
                } else {
                    tts = null;
                }
            }
        });

        messageQueue = new LinkedList<>();
        lastItem = null;
    }

    public boolean isInitialized() {
        return tts != null;
    }

    public boolean isSpeaking() {
        if (isInitialized()) {
            return tts.isSpeaking();
        }
        return false;
    }

    public boolean isScreenReaderEnabled() {
        return ! accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isEmpty();
    }


    // speak

    public enum MessageType {
        TOP_PRIORITY, INSTRUCTION, DISTANCE_OR_BEARING, TRACKED_OBJECT_MODE_DISTANCE, TRACKED_OBJECT_MODE_BEARING;
    }

    public void announce(String message, MessageType messageType) {
        if (SettingsManager.getInstance().getTtsSettings().getAnnouncementsEnabled()) {
            consumeMessage(message, messageType);
        }
    }

    public void screenReader(String message) {
        if (isScreenReaderEnabled()) {
            consumeMessage(message, MessageType.TOP_PRIORITY);
        }
    }

    public void testInstruction(float speechRate) {
        if (! isInitialized()) return;
        stopSpeaking(true);
        speak(
                new MessageQueueItem(
                    GlobalInstance.getStringResource(R.string.messageTestInstruction),
                    MessageType.TOP_PRIORITY),
                speechRate);
    }

    private synchronized void consumeMessage(String message, MessageType messageType) {
        if (! isInitialized()) return;
        MessageQueueItem newItem = new MessageQueueItem(message, messageType);
        appendToLog(
                String.format(
                    "consumeMessage: %1$s\nisSpeaking: %2$s, last: %3$s, Queue: %4$s",
                    newItem, isSpeaking(), lastItem != null ? lastItem.type : "none", printMessageQueue()));

        if (! isSpeaking()) {
            messageQueue.clear();
            tryToGetAudioFocus();
            speak(newItem);

        } else if (lastItem != null) {
            switch (newItem.type) {

                case TOP_PRIORITY:
                    stopSpeaking(true);
                    speak(newItem);
                    break;

                case INSTRUCTION:
                    if (lastItem.type == MessageType.TOP_PRIORITY
                            || lastItem.type == MessageType.INSTRUCTION) {
                        // remove all queued messages and add the current one, don't interrupt an ongoing message!
                        messageQueue.clear();
                        messageQueue.addLast(newItem);
                    } else {
                        // order is relevant here: first extract the lastItem attributes then the call stop function
                        // because the latter nulls out the lastItem vairable
                        String lastItemMessage = lastItem.message;
                        MessageType lastItemType = lastItem.type;
                        stopSpeaking(true);
                        speak(newItem);
                        // only queue an interrupted message from the distance tracking mode
                        // messages of type DISTANCE_OR_BEARING are too volatile to requeue them
                        if (lastItemType == MessageType.TRACKED_OBJECT_MODE_DISTANCE) {
                            // create a new item to get an up to date timestamp
                            messageQueue.addLast(
                                    new MessageQueueItem(lastItemMessage, lastItemType));
                        }
                    }
                    break;

                case DISTANCE_OR_BEARING:
                    // remove all queued DISTANCE_OR_BEARING messages
                    cleanUpMessageQueue(newItem.type, 0);
                    // interrupt and speak directly or add in front of the queue
                    if (lastItem.type == MessageType.DISTANCE_OR_BEARING) {
                        stopSpeaking(false);
                        speak(newItem);
                    } else {
                        // important to add it in front of the queue so it it's spoken next
                        messageQueue.addFirst(newItem);
                    }
                    break;

                case TRACKED_OBJECT_MODE_DISTANCE:
                    // remove all but the last queued TRACKED_OBJECT_MODE_DISTANCE messages
                    cleanUpMessageQueue(newItem.type, 1);
                    messageQueue.addLast(newItem);
                    break;

                case TRACKED_OBJECT_MODE_BEARING:
                    // interrupt everything but a top priority / instruction message
                    if (lastItem.type == MessageType.TOP_PRIORITY
                            || lastItem.type == MessageType.INSTRUCTION) {
                        // make sure that the new message is spoken next
                        messageQueue.clear();
                        messageQueue.addLast(newItem);
                    } else {
                        stopSpeaking(true);
                        speak(newItem);
                    }
                    break;
            }
        }

        appendToLog(
                String.format(
                    "processed: %1$s\nisSpeaking: %2$s, last: %3$s, Queue: %4$s",
                    newItem, isSpeaking(), lastItem != null ? lastItem.type : "none", printMessageQueue()));
    }

    private synchronized void speak(MessageQueueItem item) {
        speak(item, SettingsManager.getInstance().getTtsSettings().getSpeechRate());
    }

    private synchronized void speak(MessageQueueItem item, float speechRate) {
        if (item == null) return;
        appendToLog(
                String.format("speak: %1$s", item.toString()));
        lastItem = item;
        tts.setAudioAttributes(buildAudioAttributes());
        tts.setSpeechRate(speechRate);
        tts.speak(
                item.message.length() > tts.getMaxSpeechInputLength()
                ? item.message.substring(0, tts.getMaxSpeechInputLength())
                : item.message,
                TextToSpeech.QUEUE_ADD, null, item.utteranceId);
    }

    private synchronized void stopSpeaking(boolean clearMessageQueue) {
        appendToLog(
                String.format("stopSpeaking: clear queue: %1$s", clearMessageQueue));
        lastItem = null;
        if (isSpeaking()) {
            tts.stop();
        }
        if (clearMessageQueue) {
            messageQueue.clear();
        }
    }


    /**
     * UtteranceProgressListener interface inplementation
     */

    @Override public void onStart(String utteranceId) {
    }

    @Override public void onError(String utteranceId) {
        appendToLog(
                String.format("onError: utteranceId: %1$s", utteranceId));
        // here "Locale.getDefault() is intended"
        tts.setLanguage(Locale.getDefault());
        stopSpeaking(true);
        giveUpAudioFocus();
    }

    @Override public void onDone(String utteranceId) {
        if (utteranceId.equals(SILENCE_UTTERANCE_ID)) return;
        MessageQueueItem nextItem = messageQueue.pollFirst();
        appendToLog(
                String.format("onDone: utteranceId: %1$s, next: %1$s", utteranceId, nextItem));
        if (nextItem != null) {
            tts.playSilentUtterance(
                    SILENCE_DELAY, TextToSpeech.QUEUE_ADD, SILENCE_UTTERANCE_ID);
            speak(nextItem);
        } else {
            stopSpeaking(true);
            giveUpAudioFocus();
        }
    }


    /**
     * message queue
     */

    private synchronized void cleanUpMessageQueue(final MessageType typeToRemoveFromQueue, final int numberOfMessagesToKeep) {
        int counter = 0;
        for (Iterator<MessageQueueItem > it = messageQueue.descendingIterator(); it.hasNext();) {
            MessageQueueItem item = it.next();
            if (item.type == typeToRemoveFromQueue) {
                String action = "";
                if (counter < numberOfMessagesToKeep) {
                    counter++;
                    action = "skip queue item";
                } else {
                    it.remove();
                    action = "remove from queue";
                }
                appendToLog(
                        String.format(
                            Locale.ROOT, "%1$s: %2$s; for type: %3$s and counter: %4$d / keep: %5$d",
                            action, item.toString(), typeToRemoveFromQueue, counter, numberOfMessagesToKeep));
            }
        }
    }

    private synchronized String printMessageQueue() {
        String output = String.valueOf(messageQueue.size());
        if (! messageQueue.isEmpty()) {
            for (MessageQueueItem item : messageQueue) {
                output += String.format("\n    %1$s", item.toString());
            }
        }
        return output;
    }


    public class MessageQueueItem {
        public final String message;
        public final MessageType type;
        public final long timestamp;
        public final String utteranceId;

        public MessageQueueItem(String message, MessageType type) {
            this.message = message;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.utteranceId = String.format(
                    Locale.ROOT, "%1$s.%2$d", this.type.name(), this.timestamp);
        }

        @Override public String toString() {
            return String.format(
                    Locale.ROOT, "%1$s.%2$d: %3$s", this.type, this.timestamp % 10000, this.message);
        }

        @Override public int hashCode() {
            return this.utteranceId.hashCode();
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (! (obj instanceof MessageQueueItem)) {
                return false;
            }
            MessageQueueItem other = (MessageQueueItem) obj;
            return this.utteranceId.equals(other.utteranceId);
        }
    }


    /**
     * audio focus
     * only supported for android sdk >= 26 (version 8.0)
     */

    private boolean tryToGetAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return mAudioManager.requestAudioFocus(buildAudioFocusRequestForOAndNewer()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return true;
    }

    private void giveUpAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(buildAudioFocusRequestForOAndNewer());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private AudioFocusRequest buildAudioFocusRequestForOAndNewer() {
        return new AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(buildAudioAttributes())
            .setAcceptsDelayedFocusGain(false)
            .build();
    }

    private AudioAttributes buildAudioAttributes() {
        return new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(
                    isScreenReaderEnabled()
                    ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    : AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .build();
    }


    /**
     * logs and test
     */

    private void appendToLog(String message) {
        //Helper.appendToLog("tts.log", message);
    }

    private void startTestMessages() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                testAnnouncements1();
            }
        }, 2000);
    }

    private void testAnnouncements1() {
        // start announcing
        announce("In 70 metern, links auf 90°", MessageType.DISTANCE_OR_BEARING);
        sleep(500);
        // interrupt
        announce("In 60 metern, links auf 90°", MessageType.DISTANCE_OR_BEARING);
        sleep(500);
        // interrupt
        announce("Test Instruktion 1: dort hinten links abbiegen.", MessageType.INSTRUCTION);
        sleep(500);
        // queue
        announce("In 50 metern, leicht links auf 90°", MessageType.DISTANCE_OR_BEARING);
        sleep(500);
        // kill that one above and queue
        announce("Test Instruktion 2: Nun geradeaus überqueren.", MessageType.INSTRUCTION);
        sleep(500);
        // queue
        announce("Bäckerei, 30 meter, geradeaus", MessageType.TRACKED_OBJECT_MODE_DISTANCE);
        sleep(500);
        // queue but prioritise so that it is spoken before the message above
        announce("In 30 metern, leicht links auf 90°", MessageType.DISTANCE_OR_BEARING);
        sleep(500);
        // do it again and replace 30m message above
        announce("In 25 metern, leicht links auf 90°", MessageType.DISTANCE_OR_BEARING);
    }

    private void testAnnouncements2() {
        // announce
        announce("In 102 metern, leicht links auf 90°", MessageType.DISTANCE_OR_BEARING);
        sleep(100);
        // queue
        announce("Fleischerei, 43 meter, links", MessageType.TRACKED_OBJECT_MODE_DISTANCE);
        sleep(100);
        // queue
        announce("Blumenladen, 49 meter, rechts", MessageType.TRACKED_OBJECT_MODE_DISTANCE);
        sleep(100);
        // queue but cleanup queue before so that the queue still only contains two message of type distance tracking mode
        announce("Bäckerei, 51 meter, rechts", MessageType.TRACKED_OBJECT_MODE_DISTANCE);
        sleep(5000);
        // queue is empty by no
        announce("Döner, 66 meter, leicht links", MessageType.TRACKED_OBJECT_MODE_DISTANCE);
        sleep(800);
        // interrupt and speak message above afterwards again
        announce("Test Instruktion 3: dort hinten links abbiegen.", MessageType.INSTRUCTION);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

}
