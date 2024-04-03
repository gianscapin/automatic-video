package com.soflex.videoautomatico

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView


class VideoFragment : Fragment() {
    private lateinit var videoView: VideoView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)

        videoView = view.findViewById(R.id.videoView)

        // Configurar un controlador de medios para el VideoView
        val mediaController = MediaController(requireContext())
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        return view
    }

    // Método para reproducir un video dado su URI
    fun reproducirVideo(uri: Uri) {
        videoView.setVideoURI(uri)
        videoView.start()
    }
}