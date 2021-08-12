### [interface] SpikaBroacstLinstener

- onStartVideo

  - Producer

- onPerticipantUpdate

  - Array<Perticipant>

- onMicrophoneStateChanged

  - boolean

- onCameraStateChanged

  - boolean

- onSpeakerStateChanged ( only moble )

  - boolean

- onCallClosed

- onUpdateCameraDevice

  - Device

- onUpdateMicrophoneDevice

  - Device

- onUpdateSpeakerDevice

  - Device

### [class] Perticipant

#### constructor

#### public variables

- audio atream
- video steam
- mute/unmute
- display name
- avatar url

### [class] SpikaBroadcast

#### constructor

- debug
- host
- port
- roomId
- peerId
- displayName
- avatarUrl
- SpikaBroacstLinstener

#### public methods

- connect
- pause
- resume
- disconnect
- setCameraDevice
- setMicrophoneDevice
- setSpeakerDevice
- toggleCamera
- toggleMicrophone
- toggleSpeaker ( onlyMobie )
- getCameraState
- getMicrophoneState
- getSpeakerState
- switchCamera

### [class] SpikaBroadcastUI

#### constructor

- debug
- host
- port
- roomId
- peerId
- SpikaBroacstLinstener
- ParentView

#### public methods
