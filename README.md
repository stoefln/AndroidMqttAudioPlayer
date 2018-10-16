# AndroidMqttAudioPlayer
App which plays audio files from the network. Can be controlled via MQTT messages

# Commands
Even though the player can be set to listen to a different topic it defaults to "audioPlayer/control"

- "play http://some.path.to.a.audio.file" -> start streaming an audio file
- "stop" -> stop streaming file

# Planned features
- setting volume
- auto-repeat sound
