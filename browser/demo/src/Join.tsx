import React, {
  useState,
  useEffect,
  useRef,
  MutableRefObject,
  createRef,
} from "react";

function PageCreate() {
  const [videoState, setVideoState] = useState<boolean>(false);
  const [videoStream, setVideoStream] = useState<MediaStream | null>(null);
  const [microphoneState, setMicrophoneState] = useState<boolean>(false);
  const [deviceStateMessage, setDeviceStateMessage] =
    useState<string>("Waiting devices");
  const [videoReady, setVideoReady] = useState<boolean>(false);
  const [microphoneReady, setMicrophoneReady] = useState<boolean>(false);
  const videoElm: MutableRefObject<HTMLVideoElement | null> =
    useRef<HTMLVideoElement>(null);

  useEffect(() => {
    let audioAccepted: boolean = false;
    let videAccepted: boolean = false;
    // initial video
    (async () => {
      try {
        const audioStream: MediaStream =
          await navigator.mediaDevices.getUserMedia({
            audio: true,
            video: false,
          });

        const audioTrack: MediaStreamTrack = audioStream.getAudioTracks()[0];
        setMicrophoneState(true);
        setDeviceStateMessage("Microphone ready, waiting camera.");
        setMicrophoneReady(true);
        audioAccepted = true;
      } catch (e) {
        setDeviceStateMessage("Waiting camera.");
        setMicrophoneState(false);
      }

      try {
        const videoStream: MediaStream =
          await navigator.mediaDevices.getUserMedia({
            audio: false,
            video: true,
          });

        setVideoState(true);
        setVideoStream(videoStream);
        setVideoReady(true);

        if (audioAccepted) setDeviceStateMessage("Device ready.");
        else setDeviceStateMessage("Camera ready.");
      } catch (e) {
        if (audioAccepted) setDeviceStateMessage("Microphone is ready.");
        else setDeviceStateMessage("Devices are not ready.");
        setVideoState(false);
      }
    })();
  }, []);

  // when video element is ready
  useEffect(() => {
    if (videoElm && videoElm.current && videoStream) {
      videoElm.current.srcObject = videoStream;
      videoElm.current.play().catch((error: Error) => console.log(error));
    }
  }, [videoElm, videoStream]);

  return (
    <div className="App">
      {" "}
      <header></header>
      <main className="bg_color_gray">
        <div id="entry_meeting">
          <h3>Join to meeting</h3>

          <ul>
            <li>
              <input
                type="text"
                className="meeting_link"
                placeholder="Meeting link"
              />
            </li>
            <li>
              <a className="style_blue">Join</a>
            </li>
          </ul>

          <div id="camera">
            <div
              style={{ display: videoState ? "none" : "flex" }}
              className="device_state_message"
            >
              {deviceStateMessage}
            </div>
            <video
              style={{ display: videoState ? "block" : "none" }}
              ref={videoElm}
              autoPlay={true}
            />

            <ul>
              <li>
                <a className="large_icon">
                  {videoState ? (
                    <i
                      className="fas fa-video"
                      onClick={(e) => videoReady && setVideoState(!videoState)}
                    />
                  ) : (
                    <i
                      className="fas fa-video-slash"
                      onClick={(e) => videoReady && setVideoState(!videoState)}
                    />
                  )}
                </a>
              </li>
              <li>
                <a className="large_icon">
                  {microphoneState ? (
                    <i
                      className="fas fa-microphone"
                      onClick={(e) =>
                        microphoneReady && setMicrophoneState(!microphoneState)
                      }
                    />
                  ) : (
                    <i
                      className="fas fa-microphone-slash"
                      onClick={(e) =>
                        microphoneReady && setMicrophoneState(!microphoneState)
                      }
                    />
                  )}
                </a>
              </li>
            </ul>
          </div>
        </div>
      </main>
      <footer></footer>
    </div>
  );
}

export default PageCreate;
