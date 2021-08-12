import protooClient from "protoo-client";
import * as mediasoupClient from "mediasoup-client";

interface SpikaBroacstLinstener {
  onStartVideo: Function;
  onConsumerUpdate: Function;
  onMicrophoneStateChanged: Function;
  onCameraStateChanged: Function;
  onSpeakerStateChanged: Function;
  onCallClosed: Function;
  onUpdateCameraDevice: Function;
  onUpdateMicrophoneDevice: Function;
  onUpdateSpeakerDevice: Function;
}

export interface SpikaBroadcastClientConstructorInterface {
  debug: boolean;
  host: string;
  port: number;
  roomId: string;
  peerId?: string;
  listener?: SpikaBroacstLinstener;
}

export default class SpikaBroadcastClient {
  constructor({
    debug,
    host,
    port,
    roomId,
    peerId,
    listener,
  }: SpikaBroadcastClientConstructorInterface) {}

  async connect() {}
  async pause() {}
  async resume() {}
  async disconnect() {}
  async setCameraDevice() {}
  async setMicrophoneDevice() {}
  async setSpeakerDevice() {}
  async toggleCamera() {}
  async toggleMicrophone() {}
}
