package com.example.remotepdt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack

class ParticipantAdapter(meeting: Meeting) :
    RecyclerView.Adapter<ParticipantAdapter.PeerViewHolder>() {

    // creating a empty list which will store all participants
    private val participants: MutableList<Participant> = ArrayList()

    init {
        // adding the local participant(You) to the list
        participants.add(meeting.localParticipant)

        // adding Meeting Event listener to get the participant join/leave event in the meeting.
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onParticipantJoined(participant: Participant) {
                // add participant to the list
                participants.add(participant)
                notifyItemInserted(participants.size - 1)
            }

            override fun onParticipantLeft(participant: Participant) {
                var pos = -1
                for (i in participants.indices) {
                    if (participants[i].id == participant.id) {
                        pos = i
                        break
                    }
                }
                // remove participant from the list
                participants.remove(participant)
                if (pos >= 0) {
                    notifyItemRemoved(pos)
                }
            }
        })
    }

    // replace getItemCount() method with following.
    // this method returns the size of total number of participants
    override fun getItemCount(): Int {
        return participants.size
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        return PeerViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_remote_peer, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val participant = participants[position]

        holder.tvName.text = participant.displayName

        // adding the initial video stream for the participant into the 'VideoView'
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("video", ignoreCase = true)) {
                holder.participantView.visibility = View.VISIBLE
                val videoTrack = stream.track as VideoTrack
                holder.participantView.addTrack(videoTrack)
                break
            }
        }

        // add Listener to the participant which will update start or stop the video stream of that participant
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.participantView.visibility = View.VISIBLE
                    val videoTrack = stream.track as VideoTrack
                    holder.participantView.addTrack(videoTrack)
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.participantView.removeTrack()
                    holder.participantView.visibility = View.GONE
                }
            }
        })
    }

    class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 'VideoView' to show Video Stream
        var participantView: VideoView
        var tvName: TextView

        init {
            tvName = view.findViewById(R.id.tvName)
            participantView = view.findViewById(R.id.participantView)
        }
    }
}