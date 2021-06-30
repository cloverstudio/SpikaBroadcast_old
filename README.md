# SpikaBroadcast

Mediasoup client SDK's to build any kind of streaming feature.

## Features

- Broadcast audio / video streaming
- 1 to 1 audio / video 2-way streaming
- Group audio / video 2-way streaming
- mute - unmute audio, video
- Screen share ( only desctop version )
- Select device
- Limit bandwith
- Automatically adjust video/audio quality to fit to the bandwith

## Interface

### Constructor

- The pointer to the view to draw
- Server information

### Methods

- JOIN ( connect to server in this moment )
- the room indeitifier
- Get device list ( audio and video device )
- Set audio device
- Set video device
- Mute / unmute audio
- Mute / unmute video
- Start screenshare
- Stop screenshare
- Spectoator mode
- Leave
- Disconnect

### Events

#### Local event

- connect
- disconnect
- join
- error

#### Evets from other users

- connect
- join
- disconnect
- mute / unmute audio
- mute / unmute video
- audio detect
- movement detect ?
- unstable connection
