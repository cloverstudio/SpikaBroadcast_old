import React, { useEffect, useState, useRef, MutableRefObject } from "react";
import logo from "./logo.svg";
import "./App.scss";
import SpikaBroadcastWindow from "./components/spikabroadcast";
import { Link, useHistory, useParams } from "react-router-dom";
import SpikaBroadcastClient, { Participant } from "./lib/SpikaBroadcastClient";
import { types as mediasoupClientTypes } from "mediasoup-client";

function Conference() {
  let history = useHistory();

  const myVideoElm: MutableRefObject<HTMLVideoElement | null> =
    useRef<HTMLVideoElement>(null);

  const [participants, setParticipants] = useState<Array<Participant>>(null);
  const [consumerRefs, setConsumerRefs] = useState([]);
  const [cameraEnabled, setCameraEnabled] = useState<boolean>(true);
  const [micEnabled, setMicEnabled] = useState<boolean>(true);
  const [spikabroadcastClient, setSpikabroadcastClient] =
    useState<SpikaBroadcastClient>(null);

  let { roomId }: { roomId?: string } = useParams();

  useEffect(() => {
    const spikaBroadcastClient = new SpikaBroadcastClient({
      debug: true,
      host: "mediasouptest.clover.studio",
      port: 4443,
      roomId: roomId,
      peerId: "testPeer",
      displayName: "ken",
      avatarUrl: "",
      listener: {
        onStartVideo: (producer) => {
          const stream = new MediaStream();
          stream.addTrack(producer.track);
          myVideoElm.current.srcObject = stream;
          myVideoElm.current.play().catch((error: Error) => console.log(error));
        },
        onParticipantUpdate: (participants) => {
          setParticipants(Array.from(participants, ([key, val]) => val));
        },
        onMicrophoneStateChanged: (state) => {
          setMicEnabled(state);
        },
        onCameraStateChanged: (state) => {
          setCameraEnabled(state);
        },
        onSpeakerStateChanged: () => {},
        onCallClosed: () => {},
        onUpdateCameraDevice: () => {},
        onUpdateMicrophoneDevice: () => {},
        onUpdateSpeakerDevice: () => {},
      },
    });

    setSpikabroadcastClient(spikaBroadcastClient);
  }, []);

  useEffect(() => {
    if (spikabroadcastClient) spikabroadcastClient.connect();
  }, [spikabroadcastClient]);

  const consumerVideoElmInit = (elm: HTMLVideoElement, i: number) => {
    if (!participants || !participants[i] || !elm) return;

    const participant: Participant = participants[i];

    console.log("consumers", participant.consumers);

    const consumers = participant.consumers;
    if (!consumers) return;

    const stream = new MediaStream();
    consumers.map((consumer) => stream.addTrack(consumer.track));

    elm.srcObject = stream;
    elm.play().catch((error: Error) => console.log(error));
  };

  return (
    <div>
      <header></header>
      <main className="conference-main">
        <div className="peers">
          <div className="my-video">
            <video ref={myVideoElm} autoPlay={true} />
          </div>
          <>
            {participants
              ? participants.map((participant, i) => {
                  return (
                    <div className="participants-video" key={participant.id}>
                      <video
                        ref={(elm) => {
                          consumerVideoElmInit(elm, i);
                        }}
                        autoPlay={true}
                      />
                    </div>
                  );
                })
              : null}
          </>
        </div>
        <div className="controlls">
          <ul>
            <li>
              <a
                className="large_icon"
                onClick={(e) => spikabroadcastClient.toggleCamera()}
              >
                {cameraEnabled ? (
                  <i className="fas fa-video" />
                ) : (
                  <i className="fas fa-video-slash" />
                )}
              </a>
            </li>
            <li>
              <a
                className="large_icon"
                onClick={(e) => spikabroadcastClient.toggleMicrophone()}
              >
                {micEnabled ? (
                  <i className="fas fa-microphone" />
                ) : (
                  <i className="fas fa-microphone-slash" />
                )}
              </a>
            </li>
            <li>
              <a className="large_icon">
                <i className="fas fa-users"></i>
              </a>
            </li>
            <li>
              <a className="button">
                <i className="fal fa-arrow-up"></i>
              </a>
            </li>
            <li>
              <a className="button">end</a>
            </li>
          </ul>
        </div>
      </main>
      <footer></footer>
    </div>
  );
}

export default Conference;
