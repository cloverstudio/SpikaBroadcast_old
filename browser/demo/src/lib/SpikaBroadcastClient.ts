import protooClient, { Peer } from "protoo-client";
import * as mediasoupClient from "mediasoup-client";
import { types as mediasoupClientTypes } from "mediasoup-client";
import React, { useState, useEffect } from "react";
import { Logger } from "./Logger";
import deviceInfo from "./deviceInfo";
import * as e2e from "./e2e";
import { ShorthandPropertyAssignment } from "typescript";
import { timeStamp } from "console";

const PC_PROPRIETARY_CONSTRAINTS = {
  optional: [{ googDscp: true }],
};

const VIDEO_CONSTRAINS: any = {
  qvga: { width: { ideal: 320 }, height: { ideal: 240 } },
  vga: { width: { ideal: 640 }, height: { ideal: 480 } },
  hd: { width: { ideal: 1280 }, height: { ideal: 720 } },
};

// Used for VP9 webcam video.
const WEBCAM_KSVC_ENCODINGS = [{ scalabilityMode: "S3T3_KEY" }];

// Used for simulcast screen sharing.
const SCREEN_SHARING_SIMULCAST_ENCODINGS = [
  { dtx: true, maxBitrate: 1500000 },
  { dtx: true, maxBitrate: 6000000 },
];

// Used for simulcast webcam video.
const WEBCAM_SIMULCAST_ENCODINGS = [
  { scaleResolutionDownBy: 4, maxBitrate: 500000 },
  { scaleResolutionDownBy: 2, maxBitrate: 1000000 },
  { scaleResolutionDownBy: 1, maxBitrate: 5000000 },
];

const EXTERNAL_VIDEO_SRC: string =
  "https://mediasouptest.clover.studio/resources/videos/video-audio-stereo.mp4";

interface SpikaBroacstLinstener {
  onStartVideo: (producer: mediasoupClient.types.Producer) => void;
  onParticipantUpdate: (participants: Map<String, Participant>) => void;
  onMicrophoneStateChanged: (state: boolean) => void;
  onCameraStateChanged: (state: boolean) => void;
  onSpeakerStateChanged: () => void;
  onCallClosed: () => void;
  onUpdateCameraDevice: () => void;
  onUpdateMicrophoneDevice: () => void;
  onUpdateSpeakerDevice: () => void;
}

export interface Participant {
  id: string;
  displayName: string;
  consumers: Array<mediasoupClient.types.Consumer>;
}

export interface SpikaBroadcastClientConstructorInterface {
  debug: boolean;
  host: string;
  port: number;
  roomId: string;
  peerId?: string;
  displayName: string;
  avatarUrl: string;
  listener?: SpikaBroacstLinstener;
}

export default class SpikaBroadcastClient {
  // member variables
  socketUrl: string;
  logger: Logger;
  protoo: Peer;
  deviceHandlerName: mediasoupClientTypes.BuiltinHandlerName;
  mediasoupDevice: mediasoupClientTypes.Device;
  sendTransport: mediasoupClientTypes.Transport;
  recvTransport: mediasoupClientTypes.Transport;
  e2eKey: string = null;
  displayName: string = "test";
  browser: any = deviceInfo();
  micProducer: mediasoupClient.types.Producer = null;
  webcamProducer: mediasoupClient.types.Producer = null;
  webcams: Map<any, any> = null;
  webcam: any = {
    device: null,
    resolution: "hd",
  };
  forceH264: boolean = true;
  forceVP9: boolean = false;
  externalVideo: any;
  externalVideoStream: MediaStream;
  consumers: Map<String, mediasoupClient.types.Consumer> = new Map<
    String,
    mediasoupClient.types.Consumer
  >();
  listeners: SpikaBroacstLinstener;
  participants: Map<String, Participant>;
  cameraEnabled: boolean;
  micEnabled: boolean;

  // constructor
  constructor({
    debug,
    host,
    port,
    roomId,
    peerId,
    listener,
    displayName,
    avatarUrl,
  }: SpikaBroadcastClientConstructorInterface) {
    this.socketUrl = `wss://${host}:${port}/?roomId=${roomId}&peerId=${peerId}`;
    this.logger = new Logger("SpikaBroadcast", debug);
    this.logger.debug({ msg: "this.socketUrl", socketurl: this.socketUrl });
    this.listeners = listener;
    this.participants = new Map();
    this.cameraEnabled = true;
    this.micEnabled = true;
  }

  async connect() {
    const protooTransport = new protooClient.WebSocketTransport(this.socketUrl);

    this.protoo = new protooClient.Peer(protooTransport);

    this.logger.debug("constructor called");

    this.protoo.on("open", async () => {
      this.logger.debug("connected");
      this._join();
    });

    this.protoo.on("failed", () => {
      this.logger.error("connection error");
    });

    this.protoo.on("disconnected", () => {
      this.logger.debug("disconnected");
    });

    this.protoo.on("close", () => {
      this.logger.debug("closed");
    });

    this.protoo.on("request", async (request, accept, reject) => {
      this.logger.debug({
        msg: 'proto "request" event [method:%s, data:%o]',
        method: request.method,
        data: request.data,
      });

      switch (request.method) {
        case "newConsumer": {
          const {
            peerId,
            producerId,
            id,
            kind,
            rtpParameters,
            type,
            appData,
            producerPaused,
          } = request.data;

          try {
            const consumer = await this.recvTransport.consume({
              id,
              producerId,
              kind,
              rtpParameters,
              appData: { ...appData, peerId }, // Trick.
            });

            if (this.e2eKey && e2e.isSupported()) {
              e2e.setupReceiverTransform(consumer.rtpReceiver);
            }

            this.logger.debug({ msg: "new consumer", consumer });

            // Store in the map.
            this.consumers.set(consumer.id, consumer);

            consumer.on("transportclose", () => {
              const participant: Participant = this.participants.get(
                consumer.appData.peerId
              );
              if (participant)
                participant.consumers = participant.consumers.filter(
                  (c) => c.id !== consumer.id
                );
            });

            const { spatialLayers, temporalLayers } =
              mediasoupClient.parseScalabilityMode(
                consumer.rtpParameters.encodings[0].scalabilityMode
              );

            const participant: Participant = this.participants.get(
              consumer.appData.peerId
            );

            if (participant) participant.consumers.push(consumer);

            if (this.listeners.onParticipantUpdate)
              this.listeners.onParticipantUpdate(this.participants);

            // We are ready. Answer the protoo request so the server will
            // resume this Consumer (which was paused for now if video).
            accept();
          } catch (error) {
            this.logger.error({
              msg: '"newConsumer" request failed:%o',
              error,
            });

            throw error;
          }

          break;
        }

        case "newDataConsumer": {
          break;
        }
      }
    });
  }
  async pause() {}
  async resume() {}
  async disconnect() {}
  async setCameraDevice() {}
  async setMicrophoneDevice() {}
  async setSpeakerDevice() {}
  async toggleCamera() {
    if (this.cameraEnabled) await this._disableWebcam();
    else await this._enableWebcam();

    this.cameraEnabled = !this.cameraEnabled;
    if (this.listeners.onCameraStateChanged)
      this.listeners.onCameraStateChanged(this.cameraEnabled);
  }
  async toggleMicrophone() {
    if (!this.micProducer) {
      this.logger.warn("Microphone is not ready");
      return;
    }

    if (this.micEnabled) {
      // mute
      try {
        this.micProducer.pause();
        await this.protoo.request("pauseProducer", {
          producerId: this.micProducer.id,
        });

        this.micEnabled = false;

        if (this.listeners.onMicrophoneStateChanged)
          this.listeners.onMicrophoneStateChanged(this.micEnabled);
      } catch (e) {
        this.logger.error(e);
      }
    } else {
      //unmute
      try {
        this.micProducer.resume();
        await this.protoo.request("resumeProducer", {
          producerId: this.micProducer.id,
        });

        this.micEnabled = true;

        if (this.listeners.onMicrophoneStateChanged)
          this.listeners.onMicrophoneStateChanged(this.micEnabled);
      } catch (e) {
        this.logger.error(e);
      }
    }
  }

  getCameraState() {
    return this.cameraEnabled;
  }

  getMicrophoneState() {
    return this.micEnabled;
  }

  async _join() {
    this.logger.debug("start join ");

    this.mediasoupDevice = new mediasoupClient.Device({
      handlerName: this.deviceHandlerName,
    });

    const routerRtpCapabilities = await this.protoo.request(
      "getRouterRtpCapabilities"
    );

    console.log("routerRtpCapabilities", routerRtpCapabilities);

    await this.mediasoupDevice.load({ routerRtpCapabilities });
    // NOTE: Stuff to play remote audios due to browsers' new autoplay policy.
    //
    // Just get access to the mic and DO NOT close the mic track for a while.
    // Super hack!
    {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const audioTrack = stream.getAudioTracks()[0];

      audioTrack.enabled = false;

      setTimeout(() => audioTrack.stop(), 120000);
    }

    {
      const consumerTransportInfo = await this.protoo.request(
        "createWebRtcTransport",
        {
          forceTcp: true,
          producing: false,
          consuming: true,
          sctpCapabilities: this.mediasoupDevice.sctpCapabilities,
        }
      );

      const {
        id,
        iceParameters,
        iceCandidates,
        dtlsParameters,
        sctpParameters,
      } = consumerTransportInfo;

      this.recvTransport = this.mediasoupDevice.createRecvTransport({
        id,
        iceParameters,
        iceCandidates,
        dtlsParameters,
        sctpParameters,
        iceServers: [],
        additionalSettings: {
          encodedInsertableStreams: this.e2eKey,
        },
      });

      this.recvTransport.on(
        "connect",
        (
          { dtlsParameters },
          callback,
          errback // eslint-disable-line no-shadow
        ) => {
          this.logger.debug("consumer transport connected");

          this.protoo
            .request("connectWebRtcTransport", {
              transportId: this.recvTransport.id,
              dtlsParameters,
            })
            .then(callback)
            .catch(errback);
        }
      );
    }

    const transportInfo = await this.protoo.request("createWebRtcTransport", {
      forceTcp: true,
      producing: true,
      consuming: false,
      sctpCapabilities: this.mediasoupDevice.sctpCapabilities,
    });

    const { id, iceParameters, iceCandidates, dtlsParameters, sctpParameters } =
      transportInfo;

    this.sendTransport = this.mediasoupDevice.createSendTransport({
      id,
      iceParameters,
      iceCandidates,
      dtlsParameters,
      sctpParameters,
      iceServers: [],
      proprietaryConstraints: PC_PROPRIETARY_CONSTRAINTS,
      additionalSettings: {
        encodedInsertableStreams: this.e2eKey,
      },
    });

    this.sendTransport.on(
      "connect",
      async (
        { dtlsParameters },
        callback,
        errback // eslint-disable-line no-shadow
      ) => {
        this.logger.debug("transport connected");

        const params = await this.protoo.request("connectWebRtcTransport", {
          transportId: this.sendTransport.id,
          dtlsParameters,
        });

        callback();
      }
    );

    this.sendTransport.on(
      "produce",
      async ({ kind, rtpParameters, appData }, callback, errback) => {
        this.logger.debug("transport produce");

        const { id } = await this.protoo.request("produce", {
          transportId: this.sendTransport.id,
          kind,
          rtpParameters,
          appData,
        });

        callback({ id });
      }
    );

    this.sendTransport.on(
      "producedata",
      async (
        { sctpStreamParameters, label, protocol, appData },
        callback,
        errback
      ) => {
        try {
          // eslint-disable-next-line no-shadow
          const { id } = await this.protoo.request("produceData", {
            transportId: this.sendTransport.id,
            sctpStreamParameters,
            label,
            protocol,
            appData,
          });

          callback({ id });
        } catch (error) {
          errback(error);
        }
      }
    );

    this.sendTransport.on("connectionstatechange", (connectionState) => {
      this.logger.debug({ msg: "connectionstatechange", connectionState });
    });

    const { peers } = await this.protoo.request("join", {
      displayName: this.displayName,
      device: this.browser,
      rtpCapabilities: this.mediasoupDevice.rtpCapabilities,
      sctpCapabilities: this.mediasoupDevice.sctpCapabilities,
    });

    peers.map((peer: any) => {
      this.participants.set(peer.id, {
        id: peer.id,
        displayName: peer.displayName,
        consumers: [],
      });

      this.logger.debug({ msg: "new peer", id: peer.id });
    });

    if (this.listeners.onParticipantUpdate)
      this.listeners.onParticipantUpdate(this.participants);

    this._enableMic();
    this._enableWebcam();

    this.protoo.on("notification", (notification) => {
      this.logger.debug({
        msg: 'proto "notification" event [method:%s, data:%o]',
        notificaitonmsg: notification.method,
        notificationdata: notification.data,
      });

      switch (notification.method) {
        case "producerScore": {
          break;
        }
        case "newPeer": {
          const peer = notification.data;

          console.log("this.micProducer", this.micProducer);
          this.logger.debug({ msg: "new peer", id: peer.id });

          this.participants.set(peer.id, {
            id: peer.id,
            displayName: peer.displayName,
            consumers: [],
          });

          if (this.listeners.onParticipantUpdate)
            this.listeners.onParticipantUpdate(this.participants);

          break;
        }
        case "peerClosed": {
          const peerId = notification.data.peerId;
          this.participants.delete(peerId);

          this.logger.debug({ msg: "peer closed", peerId, id: peerId });

          if (this.listeners.onParticipantUpdate)
            this.listeners.onParticipantUpdate(this.participants);

          break;
        }
        case "peerDisplayNameChanged": {
          break;
        }
        case "consumerClosed": {
          const { consumerId } = notification.data;
          const consumer = this.consumers.get(consumerId);
          if (!consumer) break;
          consumer.close();
          this.consumers.delete(consumerId);

          const participant = this.participants.get(consumer.appData.peerId);
          if (participant)
            participant.consumers = participant.consumers.filter(
              (c) => c.id !== consumer.id
            );

          if (this.listeners.onParticipantUpdate)
            this.listeners.onParticipantUpdate(this.participants);

          break;
        }
        case "consumerPaused": {
          const { consumerId } = notification.data;
          const consumer = this.consumers.get(consumerId);

          console.log("consumerPaused", consumer);
          if (!consumer) break;

          break;
        }
        case "consumerResumed": {
          break;
        }
        case "consumerLayersChanged": {
          break;
        }
        case "consumerScore": {
          break;
        }
        case "dataConsumerClosed": {
          break;
        }
        case "activeSpeaker": {
          break;
        }
        default: {
          this.logger.error(
            `unknown protoo notification.method ${notification.method}`
          );
        }
      }
    });
  }

  async _enableMic() {
    this.logger.debug("enableMic()");

    if (this.micProducer) return;

    if (!this.mediasoupDevice.canProduce("audio")) {
      this.logger.error("enableMic() | cannot produce audio");
      return;
    }

    let track;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
      });

      track = stream.getAudioTracks()[0];

      this.micProducer = await this.sendTransport.produce({
        track,
        codecOptions: {
          opusStereo: true,
          opusDtx: true,
        },
        // NOTE: for testing codec selection.
        // codec : this._mediasoupDevice.rtpCapabilities.codecs
        // 	.find((codec) => codec.mimeType.toLowerCase() === 'audio/pcma')
      });

      if (this.e2eKey && e2e.isSupported()) {
        e2e.setupSenderTransform(this.micProducer.rtpSender);
      }

      this.micProducer.on("newtransport", () => {
        this.logger.debug("new mic transport");
      });

      this.micProducer.on("transportclose", () => {
        this.micProducer = null;
      });

      this.micProducer.on("trackended", () => {
        this._disableMic().catch(() => {});
      });
    } catch (error) {
      this.logger.error({ msg: "enableMic() | failed:%o", error });
      if (track) track.stop();
    }
  }

  async _disableMic() {
    this.logger.debug("disableMic()");

    if (!this.micProducer) return;

    this.micProducer.close();

    try {
      await this.protoo.request("closeProducer", {
        producerId: this.micProducer.id,
      });
    } catch (error) {
      this.logger.error(error);
    }

    this.micProducer = null;
  }

  async _getExternalVideoStream() {
    if (this.externalVideoStream) return this.externalVideoStream;

    if (this.externalVideo.readyState < 3) {
      await new Promise((resolve) =>
        this.externalVideo.addEventListener("canplay", resolve)
      );
    }

    if (this.externalVideo.captureStream)
      this.externalVideoStream = this.externalVideo.captureStream();
    else if (this.externalVideo.mozCaptureStream)
      this.externalVideoStream = this.externalVideo.mozCaptureStream();
    else throw new Error("video.captureStream() not supported");

    return this.externalVideoStream;
  }

  async _enableWebcam() {
    this.logger.debug("enableWebcam()");

    if (this.webcamProducer) return;

    if (!this.mediasoupDevice.canProduce("video")) {
      this.logger.error("enableWebcam() | cannot produce video");
      return;
    }

    let track;
    let device;

    try {
      if (!this.externalVideo) {
        await this._updateWebcams();
        device = this.webcam.device;

        const { resolution } = this.webcam;

        if (!device) throw new Error("no webcam devices");

        this.logger.debug("enableWebcam() | calling getUserMedia()");

        const stream: MediaStream = await navigator.mediaDevices.getUserMedia({
          video: {
            deviceId: { ideal: device.deviceId },
            ...VIDEO_CONSTRAINS[resolution],
          },
        });

        track = stream.getVideoTracks()[0];
        this.logger.debug(track);
      } else {
        device = { label: "external video" };
        const stream = await this._getExternalVideoStream();
        track = stream.getVideoTracks()[0].clone();
      }

      let encodings;
      let codec;
      const codecOptions = {
        videoGoogleStartBitrate: 1000,
      };

      if (this.forceH264) {
        codec = this.mediasoupDevice.rtpCapabilities.codecs.find(
          (c) => c.mimeType.toLowerCase() === "video/h264"
        );

        if (!codec) {
          throw new Error("desired H264 codec+configuration is not supported");
        }
      } else if (this.forceVP9) {
        codec = this.mediasoupDevice.rtpCapabilities.codecs.find(
          (c) => c.mimeType.toLowerCase() === "video/vp9"
        );

        if (!codec) {
          throw new Error("desired VP9 codec+configuration is not supported");
        }
      }

      // If VP9 is the only available video codec then use SVC.
      const firstVideoCodec = this.mediasoupDevice.rtpCapabilities.codecs.find(
        (c) => c.kind === "video"
      );

      if (
        (this.forceVP9 && codec) ||
        firstVideoCodec.mimeType.toLowerCase() === "video/vp9"
      ) {
        encodings = WEBCAM_KSVC_ENCODINGS;
      } else {
        encodings = WEBCAM_SIMULCAST_ENCODINGS;
      }

      this.webcamProducer = await this.sendTransport.produce({
        track,
        encodings,
        codecOptions,
        codec,
      });

      if (this.listeners.onStartVideo)
        this.listeners.onStartVideo(this.webcamProducer);

      if (this.e2eKey && e2e.isSupported()) {
        e2e.setupSenderTransform(this.webcamProducer.rtpSender);
      }

      this.webcamProducer.on("newtransport", () => {
        this.logger.debug("new web cam transport");
      });

      this.webcamProducer.on("transportclose", () => {
        this.webcamProducer = null;
      });

      this.webcamProducer.on("trackended", () => {
        this._disableWebcam().catch(() => {});
      });

      this.webcamProducer.observer.on("close", () => {
        this.logger.debug("web cam producer close");
      });

      this.webcamProducer.observer.on("pause", () => {
        this.logger.debug("web cam producer pause");
      });

      this.webcamProducer.observer.on("resume", () => {
        this.logger.debug("web cam producer resume");
      });

      this.webcamProducer.observer.on("trackended", () => {
        this.logger.debug("web cam producer trackended");
      });
    } catch (error) {
      this.logger.error({ msg: "enableWebcam() | failed:%o", error });

      if (track) track.stop();
    }
  }

  async _disableWebcam() {
    this.logger.debug("disableWebcam()");

    if (!this.webcamProducer) return;

    this.webcamProducer.close();

    try {
      await this.protoo.request("closeProducer", {
        producerId: this.webcamProducer.id,
      });
    } catch (error) {
      this.logger.error(error);
    }

    this.webcamProducer = null;
  }

  async _updateWebcams() {
    this.logger.debug("_updateWebcams()");

    // Reset the list.
    this.webcams = new Map();
    this.logger.debug("_updateWebcams() | calling enumerateDevices()");

    const devices = await navigator.mediaDevices.enumerateDevices();

    for (const device of devices) {
      if (device.kind !== "videoinput") continue;

      this.webcams.set(device.deviceId, device);
    }

    const array = Array.from(this.webcams.values());
    const len = array.length;
    const currentWebcamId = this.webcam.device
      ? this.webcam.device.deviceId
      : undefined;

    this.logger.debug({ msg: "_updateWebcams() [webcams:%o]", array });

    if (len === 0) this.webcam.device = null;
    else if (!this.webcams.has(currentWebcamId)) this.webcam.device = array[0];
  }
}
