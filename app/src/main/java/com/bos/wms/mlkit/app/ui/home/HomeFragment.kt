package com.bos.wms.mlkit.app.ui.home

import Model.UserLoginModel
import Remote.APIClient
import Remote.BasicApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.AlarmClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bos.wms.mlkit.General
import com.bos.wms.mlkit.R
import com.bos.wms.mlkit.app.*
import com.bos.wms.mlkit.databinding.FragmentHomeBinding
import com.bos.wms.mlkit.storage.Storage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_locationcheck.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        root.btnMenuPicking.setOnClickListener {
            ProceedPickingClick()
        }

        root.btnMenuNextReceivingStatus.setOnClickListener {
            ProceedNextReceivingStatus()
        }
        root.btnLocationCheck.setOnClickListener {
            ProceedLocationCheckClick()
        }
        root.btnMenuFillPallete.setOnClickListener {
            ProceedFillPallete()
        }
        root.btnMenuPutAwayPallete.setOnClickListener {
            ProceedPutAwayPallete()
        }
        root.btnFoldingScan.setText("Serial Generator")
        root.btnFoldingScan.setOnClickListener {
            ProceedFoldingScan()
        }
        root.btnMenuShipmentPalleteReceiving.setOnClickListener {
            ProceedShipmentPalleteReceiving()
        }
        root.btnMenuShipmentCartonReceiving.setOnClickListener {
            ProceedShipmentCartonReceiving()
        }
        root.btnMenuShipmentReceivingPalleteCount.setOnClickListener {
            ProceedShipmentReceivingPalleteCount()
        }
        root.btnMenuItemPricing.setOnClickListener {
            ProceedItemPricingActivity()
        }
        root.btnSerialMissing.setOnClickListener {
            ProceedMissingItem()
        }


        mStorage= Storage(context?.applicationContext) //sp存储
        IPAddress = mStorage.getDataString("IPAddress", "192.168.10.82")

        return root
    }
    lateinit var api: BasicApi
    var compositeDisposable= CompositeDisposable()
    lateinit var  mStorage:Storage ;  //sp存储
    var IPAddress =""
      fun ProceedPickingClick() {

        try {
            // TODO: handle loggedInUser authentication


            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            compositeDisposable.addAll(
                api.AssignPickingList(General.getGeneral(context).UserID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                        if(s){
                            txtStatus.setText("")
                            val intent = Intent (getActivity(), PickingActivity::class.java)
                            startActivity(intent)
                        }
                        },
                        {t:Throwable?->
                            run {
                                txtStatus.setText(t!!.message)
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("ProceedPickingClick", e))
        }
        finally {
        }
    }

    fun ProceedLocationCheckClick() {

        try {
            // TODO: handle loggedInUser authentication

            api= APIClient.getInstance(IPAddress,false).create(BasicApi::class.java)
            var FloorID: Int=General.getGeneral(context?.applicationContext).FloorID
            compositeDisposable.addAll(
                api.AssignLocationCheck(General.getGeneral(context).UserID,FloorID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->
                            var ErrorMsg = ""
                            try {
                                ErrorMsg = s.string()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if (ErrorMsg.isEmpty()) {
                                val intent = Intent (getActivity(), LocationCheckActivity::class.java)
                                startActivity(intent)
                                txtStatus.setText("")
                            } else if (!ErrorMsg.isEmpty()) {
                                txtStatus.setTextColor(Color.RED)
                                txtStatus.setText(ErrorMsg)
                                //(ErrorMsg)
                            }


                        },
                        {t:Throwable?->
                            run {
                                txtStatus.setTextColor(Color.RED)
                                txtStatus.setText(t!!.message.toString())
                            }
                        }
                    )
            )
        } catch (e: Throwable) {
            throw(IOException("ProceedLocationCheckClick", e))
        }
        finally {
        }
    }

    fun ProceedNextReceivingStatus() {
        val intent = Intent (getActivity(), NextReceivingStatusActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedFoldingScan() {
        val intent = Intent (getActivity(), SerialGeneratorActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedShipmentPalleteReceiving() {
        val intent = Intent (getActivity(), ShipmentPalleteReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedShipmentCartonReceiving() {
        val intent = Intent (getActivity(), ShipmentCartonReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedShipmentReceivingPalleteCount() {
        val intent = Intent (getActivity(), ShipmentCartonCountReceivingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedItemPricingActivity() {
        val intent = Intent (getActivity(), ItemPricingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedMissingItem() {
        val intent = Intent (getActivity(), ItemSerialMissingActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    fun ProceedPutAwayPallete() {
        val intent = Intent (getActivity(), PutAwayPalleteActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }

    fun ProceedFillPallete() {
        val intent = Intent (getActivity(), FillPalleteActivity::class.java)
        startActivity(intent)
        txtStatus.setText("")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}