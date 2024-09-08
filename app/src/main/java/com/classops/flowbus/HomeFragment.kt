package com.classops.flowbus

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.classops.flowbus.databinding.FragmentHomeBinding
import com.github.classops.flowbus.FlowBus

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = this._binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnFlow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setMaxLifecycle(this, Lifecycle.State.STARTED)
                .hide(this)
                .commit()
        }
        FlowBus.on<Int>(viewLifecycleOwner,"count", sticky = true) {
            Log.e("Home", "home count: $it")
        }

        this.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.e("Home", "state: ${source.lifecycle.currentState}")
            }

        })
    }

}