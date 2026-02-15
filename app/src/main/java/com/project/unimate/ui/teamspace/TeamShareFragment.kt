package com.project.unimate.ui.teamspace



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController

import com.project.unimate.R



class TeamShareFragment : Fragment() {



    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View? {

        return inflater.inflate(R.layout.fragment_team_share, container, false)

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.teamShareBack).setOnClickListener {

            findNavController().popBackStack()

        }

    }

}
