package com.bos.wms.mlkit.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.app.bosApp.PackingActivity
import com.bos.wms.mlkit.app.bosApp.PackingDCActivity
import com.bos.wms.mlkit.databinding.FragmentDashboardBinding
import com.bos.wms.mlkit.storage.Storage
import kotlinx.android.synthetic.main.fragment_dashboard.view.*


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root





       root.btnDestination.setOnClickListener{
           val intent = Intent (getActivity(), PackingActivity::class.java)
           startActivity(intent)
       }


        mStorage= Storage(context?.applicationContext)
        var FloorID: Int= General.getGeneral(context?.applicationContext).FloorID
        var UserId = General.getGeneral(context).UserID
        root.textBranch.setText(General.getGeneral(context).LocationString);
        root.textUser.setText(""+ General.getGeneral(context).UserID + " " + General.getGeneral(context).UserName);

        root.textBranch.isEnabled=false;
        root.textBranch.isEnabled=false;

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    lateinit var  mStorage:Storage ;
}