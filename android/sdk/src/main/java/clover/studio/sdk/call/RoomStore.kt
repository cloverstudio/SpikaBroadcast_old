package clover.studio.sdk.call

import android.text.TextUtils
import androidx.core.util.Supplier
import androidx.lifecycle.MutableLiveData
import clover.studio.sdk.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Producer

/**
 * Room state.
 *
 *
 * Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
class RoomStore {
    // room
    // mediasoup-demo/app/lib/redux/reducers/room.js
    private val roomInfo: SupplierMutableLiveData<RoomInfo> = SupplierMutableLiveData { RoomInfo() }

    // me
    // mediasoup-demo/app/lib/redux/reducers/me.js
    private val me: SupplierMutableLiveData<Me> = SupplierMutableLiveData(Supplier { Me() })

    // producers
    // mediasoup-demo/app/lib/redux/reducers/producers.js
    private val producers: SupplierMutableLiveData<Producers> = SupplierMutableLiveData(
        Supplier { Producers() })

    // peers
    // mediasoup-demo/app/lib/redux/reducers/peer.js
    private val peers: SupplierMutableLiveData<Peers> =
        SupplierMutableLiveData(Supplier { Peers() })

    // consumers
    // mediasoup-demo/app/lib/redux/reducers/consumers.js
    private val consumers: SupplierMutableLiveData<Consumers> = SupplierMutableLiveData(
        Supplier { Consumers() })

    // notify
    // mediasoup-demo/app/lib/redux/reducers/notifications.js
    private val notify: MutableLiveData<Notify> = MutableLiveData<Notify>()
    fun setRoomUrl(roomId: String?, url: String?) {
        roomInfo.postValue { roomInfo ->
            roomInfo.roomId = roomId
            roomInfo.url = url
        }
    }

    fun setRoomState(state: ConnectionState) {
        roomInfo.postValue { roomInfo -> roomInfo.connectionState = state }
        if (ConnectionState.CLOSED == state) {
            peers.postValue(Peers::clear)
            me.postValue(Me::clear)
            producers.postValue(Producers::clear)
            consumers.postValue(Consumers::clear)
        }
    }

    fun setRoomActiveSpeaker(peerId: String?) {
        roomInfo.postValue { roomInfo: RoomInfo ->
            roomInfo.activeSpeakerId = peerId
        }
    }

    fun setRoomStatsPeerId(peerId: String?) {
        roomInfo.postValue { roomInfo -> roomInfo.statsPeerId = peerId }
    }

    fun setRoomFaceDetection(enable: Boolean) {
        roomInfo.postValue { roomInfo -> roomInfo.isFaceDetection = enable }
    }

    fun setMe(peerId: String?, displayName: String?, device: DeviceInfo?) {
        me.postValue { me ->
            me.id = peerId
            me.displayName = displayName
            me.device = device
        }
    }

    fun setMediaCapabilities(canSendMic: Boolean, canSendCam: Boolean) {
        me.postValue { me ->
            me.isCanSendMic = canSendMic
            me.isCanSendCam = canSendCam
        }
    }

    fun setCanChangeCam(canChangeCam: Boolean) {
        me.postValue { me -> me.isCanSendCam = canChangeCam }
    }

    fun setDisplayName(displayName: String?) {
        me.postValue { me -> me.displayName = displayName }
    }

    fun setAudioOnlyState(enabled: Boolean) {
        me.postValue { me -> me.isAudioOnly = enabled }
    }

    fun setAudioOnlyInProgress(enabled: Boolean) {
        me.postValue { me -> me.isAudioOnlyInProgress = enabled }
    }

    fun setAudioMutedState(enabled: Boolean) {
        me.postValue { me -> me.isAudioMuted = enabled }
    }

    fun setRestartIceInProgress(restartIceInProgress: Boolean) {
        me.postValue { me -> me.isRestartIceInProgress = restartIceInProgress }
    }

    fun setCamInProgress(inProgress: Boolean) {
        me.postValue { me -> me.isCamInProgress = inProgress }
    }

    fun addProducer(producer: Producer?) {
        producers.postValue { producers ->
            producer?.let {
                producers.addProducer(it)
            }
        }
    }

    fun setProducerPaused(producerId: String?) {
        producers.postValue { producers ->
            producerId?.let {
                producers.setProducerPaused(it)
            }
        }
    }

    fun setProducerResumed(producerId: String?) {
        producers.postValue { producers ->
            producerId?.let {
                producers.setProducerResumed(it)
            }
        }
    }

    fun removeProducer(producerId: String?) {
        producers.postValue { producers ->
            producerId?.let {
                producers.removeProducer(it)
            }
        }
    }

    fun setProducerScore(producerId: String, score: JSONArray?) {
        producers.postValue { producers -> producers.setProducerScore(producerId, score) }
    }

    fun addDataProducer(dataProducer: Any?) {
        // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
    }

    fun removeDataProducer(dataProducerId: String?) {
        // TODO(HaiyangWU): support data consumer.
    }

    fun addPeer(peerId: String, peerInfo: JSONObject) {
        peers.postValue { peersInfo: Peers ->
            peersInfo.addPeer(peerId, peerInfo)
        }
    }

    fun setPeerDisplayName(peerId: String, displayName: String?) {
        peers.postValue { peersInfo: Peers ->
            peersInfo.setPeerDisplayName(peerId, displayName)
        }
    }

    fun removePeer(peerId: String) {
        roomInfo.postValue { roomInfo: RoomInfo ->
            if (!TextUtils.isEmpty(peerId) && peerId == roomInfo.activeSpeakerId) {
                roomInfo.activeSpeakerId = null
            }
            if (!TextUtils.isEmpty(peerId) && peerId == roomInfo.statsPeerId) {
                roomInfo.statsPeerId = null
            }
        }
        peers.postValue { peersInfo: Peers ->
            peersInfo.removePeer(
                peerId
            )
        }
    }

    fun addConsumer(peerId: String, type: String, consumer: Consumer, remotelyPaused: Boolean) {
        consumers.postValue { consumers: Consumers ->
            consumers.addConsumer(
                type,
                consumer,
                remotelyPaused
            )
        }
        peers.postValue { peers ->
            peers.addConsumer(
                peerId,
                consumer
            )
        }
    }

    fun removeConsumer(peerId: String, consumerId: String) {
        consumers.postValue { consumers: Consumers ->
            consumers.removeConsumer(
                consumerId
            )
        }
        peers.postValue { peers ->
            peers.removeConsumer(
                peerId,
                consumerId
            )
        }
    }

    fun setConsumerPaused(consumerId: String, originator: String) {
        consumers.postValue { consumers: Consumers ->
            consumers.setConsumerPaused(
                consumerId,
                originator
            )
        }
    }

    fun setConsumerResumed(consumerId: String, originator: String) {
        consumers.postValue { consumers: Consumers ->
            consumers.setConsumerResumed(
                consumerId,
                originator
            )
        }
    }

    fun setConsumerCurrentLayers(consumerId: String, spatialLayer: Int, temporalLayer: Int) {
        consumers.postValue { consumers: Consumers ->
            consumers.setConsumerCurrentLayers(
                consumerId,
                spatialLayer,
                temporalLayer
            )
        }
    }

    fun setConsumerScore(consumerId: String, score: JSONArray?) {
        consumers.postValue { consumers: Consumers ->
            consumers.setConsumerScore(
                consumerId,
                score
            )
        }
    }

    fun addDataConsumer(peerId: String, dataConsumer: Any?) {
        // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
    }

    fun removeDataConsumer(peerId: String, dataConsumerId: String) {
        // TODO(HaiyangWU): support data consumer.
    }

    fun addNotify(text: String) {
        notify.postValue(Notify("info", text))
    }

    fun addNotify(text: String, timeout: Int) {
        notify.postValue(Notify("info", text, timeout))
    }

    fun addNotify(type: String, text: String) {
        notify.postValue(Notify(type, text))
    }

    fun addNotify(text: String, throwable: Throwable) {
        notify.postValue(Notify("error", text + throwable.message))
    }

    fun getRoomInfo(): SupplierMutableLiveData<RoomInfo> {
        return roomInfo
    }

    fun getMe(): SupplierMutableLiveData<Me> {
        return me
    }

    fun getNotify(): MutableLiveData<Notify> {
        return notify
    }

    fun getPeers(): SupplierMutableLiveData<Peers> {
        return peers
    }

    fun getProducers(): SupplierMutableLiveData<Producers> {
        return producers
    }

    fun getConsumers(): SupplierMutableLiveData<Consumers> {
        return consumers
    }

    companion object {
        private const val TAG = "RoomStore"
    }
}