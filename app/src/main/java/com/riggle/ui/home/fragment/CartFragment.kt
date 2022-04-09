package com.riggle.ui.home.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.riggle.R
import com.riggle.data.models.APICommonResponse
import com.riggle.data.models.ApiError
import com.riggle.data.models.request.ProductCartRequest
import com.riggle.data.models.request.VariantUpdate
import com.riggle.data.models.response.EditCartResponse
import com.riggle.data.models.response.ProductsData
import com.riggle.data.models.response.ResponseCartData
import com.riggle.data.models.response.UserDetails
import com.riggle.data.network.ApiResponseListener
import com.riggle.ui.base.connector.CustomAppViewConnector
import com.riggle.ui.base.fragment.CustomAppFragmentViewImpl
import com.riggle.ui.dialogs.LoadingDialog
import com.riggle.ui.home.adapters.CartAdapter
import com.riggle.ui.other.SelectDeliverySlot
import com.riggle.utils.UserProfileSingleton
import kotlinx.android.synthetic.main.fragment_cart.*
import org.koin.android.ext.android.inject
import java.util.*


class CartFragment : CustomAppFragmentViewImpl(), CustomAppViewConnector,
    CartAdapter.HomeProductsListener {

    private val userPreference: UserProfileSingleton by inject()

    private var cartData: ResponseCartData? = null
    private var cartAdapter: CartAdapter? = null
    private var isRiggleCoinApplied = false
    private var loaderDialog: LoadingDialog? = null
    private var avail_riggle_coin = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        connectViewToParent(this)
        super.onCreateView(inflater, container, savedInstanceState)
        return view
    }

    override fun setView(): Int {
        return R.layout.fragment_cart
    }

    override fun initializeViews(savedInstanceState: Bundle?) {
        cbRiggleCoins?.setOnCheckedChangeListener { compoundButton, b -> if (b) applyRiggleCoin() else removeRiggleCoin() }

        tvProceed.setOnClickListener {
            activity?.let {
                val coin = if (isRiggleCoinApplied) avail_riggle_coin.toFloat() else 0f
                if (isRiggleCoinApplied) {
                    if (TextUtils.isEmpty(
                            tvAvailableCoins.text.toString()
                                .trim()
                        )
                    ) {
                        tvAvailableCoins.error = "Please Enter Riggle Coin"
                        return@setOnClickListener
                    }
                    /*if (tvAvailableCoins.text.toString()
                            .trim() != null && tvAvailableCoins.text.toString()
                            .toInt() <= coin
                    ) {*/
                    SelectDeliverySlot.start(
                        it,
                        isRiggleCoinApplied,
                        tvTotalAmountValue?.text.toString(),
                        tvAvailableCoins.text.toString()
                            .toDouble()
                    )
                    /*} else {
                        Toast.makeText(
                            requireContext(),
                            "You do not have that much enough coin.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }*/
                } else {
                    SelectDeliverySlot.start(
                        it,
                        isRiggleCoinApplied,
                        tvTotalAmountValue?.text.toString(),
                        0.0
                    )
                }
            }
        }
    }

    fun loadTab() {
        fetchCart()
    }

    private fun fetchCart() {
        showHideLoader(true)
        /*dataManager.fetchCart(object : ApiResponseListener<ResponseCartData> {
            override fun onSuccess(response: ResponseCartData) {
                showHideLoader(false)
                response?.let {
                    if (response != null) {
                        hideEmptyCartView()
                        cartData = response
                        populateCartView()
                        //populateCartDetails(response.data?.cart_details)
                        cartLinearLayout?.visibility = View.VISIBLE
                    } else {
                        showEmptyCartView()
                    }
                }

            }

            override fun onError(apiError: ApiError?) {
                showHideLoader(false)
            }
        },userPreference.userData?.retailer?.id)*/

        dataManager.fetchCart(object : ApiResponseListener<ResponseCartData> {
            override fun onSuccess(response: ResponseCartData) {
                showHideLoader(false)
                response?.let {
                    if (response != null && response.products_in_cart != null && !response.products_in_cart.isEmpty()) {
                        hideEmptyCartView()
                        cartData = response
                        populateCartView()
                        populateCartDetails(response)
                        cartLinearLayout?.visibility = View.VISIBLE
                    } else {
                        showEmptyCartView()
                    }
                }

            }

            override fun onError(apiError: ApiError?) {
                showHideLoader(false)
            }
        }, userPreference.userData?.retailer?.id ?: 0, "banner_image,service_hub")
        /*schemes.free_product,*/
    }

    private fun showEmptyCartView() {
        emptyCartLinearLayout?.visibility = View.VISIBLE
    }

    private fun hideEmptyCartView() {
        emptyCartLinearLayout?.visibility = View.GONE
    }

    private fun populateCartView() {

        activity?.let {
            cartData?.let { cartData ->
                cartAdapter = CartAdapter(it, cartData.products_in_cart)
                rvCart?.layoutManager = LinearLayoutManager(activity)
                (Objects.requireNonNull(rvCart?.itemAnimator) as SimpleItemAnimator).supportsChangeAnimations =
                    false
                rvCart?.adapter = cartAdapter
                cartAdapter?.setListener(this)
            }
        }
    }

    private fun populateCartDetails(cartDetails: ResponseCartData) {
        if (cartDetails != null) {
            //tvDeliveryEst?.text = cartDetails.estimated_delivery
            tvPriceItems?.text = String.format(
                activity?.getString(R.string.price_value_items) ?: "",
                cartData?.products_in_cart?.size
            )
            tvCartPrice?.text = String.format(
                activity?.getString(R.string.rupees_value_double) ?: "",
                Math.round(cartDetails.final_amount).toFloat()
            )
            if ((cartDetails?.amount - cartDetails?.final_amount) == 0.0) {
                tvDiscountValue?.text = String.format(
                    activity?.getString(R.string.rupees_value_double) ?: "",
                    Math.round((cartDetails?.amount - cartDetails?.final_amount)).toFloat()
                )
                tvDiscountValue?.visibility = View.GONE
                tvDiscount?.visibility = View.GONE
            } else {
                tvDiscountValue?.text = getString(R.string.minus_with_space) + String.format(
                    activity?.getString(R.string.rupees_value) ?: "",
                    Math.round((cartDetails?.amount - cartDetails?.final_amount)).toFloat()
                )
                tvDiscountValue?.visibility = View.VISIBLE
                tvDiscount?.visibility = View.VISIBLE
            }

            /*if (cartDetails.redeemed_riggle_coins != 0f) {
                showAvailableCoinsView()
                tvAvailableCoins?.text = String.format(
                    activity?.getString(R.string.available_coins_value) ?: "",
                    cartDetails.redeemed_riggle_coins
                )
            } else {
                hideAvailableCoinsView()
            }*/

            if (isRiggleCoinApplied) {
                /*cartDetails.final_amount?.let {
                    cartData?.cart_details?.riggle_coins_discount?.let {
                        val amt = cartDetails.final_amount - it.toDouble()
                        tvTotalAmountValue?.text = String.format(
                            activity?.getString(R.string.rupees_value_double) ?: "",
                            amt
                        )
                    }

                }*/
                tvTotalAmountValue?.text = String.format(
                    activity?.getString(R.string.rupees_value_double) ?: "",
                    Math.round(cartDetails.final_amount).toFloat()
                )
            } else {
                tvTotalAmountValue?.text = String.format(
                    activity?.getString(R.string.rupees_value_double) ?: "",
                    Math.round(cartDetails.final_amount).toFloat()
                )
            }

            if (cartDetails.margin != null) tvCartProfit?.text =
                "Grand profit ₹" + Math.round(cartDetails.margin)
            //Html.fromHtml(cartDetails.total_profit) else tvCartProfit?.visibility = View.GONE

            tvCheckoutPrice?.text = tvTotalAmountValue?.text.toString()
            /*tvRiggleCoins?.text = activity?.getString(R.string.earn_value)?.let {
                String.format(
                    it,
                    cartDetails.riggle_coins
                )
            }*/
            tvRiggleCoins?.text = "Earn " + cartDetails.riggle_coins
        }
    }

    private fun hideAvailableCoinsView() {
        tvAvailableCoins.visibility = View.GONE
        cbRiggleCoins.visibility = View.GONE
    }

    private fun showAvailableCoinsView() {
        tvAvailableCoins.visibility = View.VISIBLE
        cbRiggleCoins.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun applyRiggleCoin() {
        try {
            isRiggleCoinApplied = true
            /*val finalAmount =
                (cartData?.cart_details?.final_amount
                    ?: 0.0f).toInt() - (cartData?.cart_details?.riggle_coins_discount ?: 0.0f)
            tvTotalAmountValue?.text =
                String.format(activity?.getString(R.string.rupees_value_double) ?: "", finalAmount)
            tvCheckoutPrice?.text = tvTotalAmountValue?.text.toString()
            isRiggleCoinApplied = true
            tvRiggleDiscountValue?.text = getString(R.string.minus_with_space) + String.format(
                activity?.getString(R.string.rupees_value) ?: "", Math.round(
                    cartData?.cart_details?.riggle_coins_discount ?: 0.0f
                )
            )
            if (Math.round(cartData?.cart_details?.riggle_coins_discount ?: 0f) == 0) {
                tvRiggleDiscountValue?.visibility = View.GONE
            } else {
                tvRiggleDiscountValue?.visibility = View.VISIBLE
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeRiggleCoin() {
        tvTotalAmountValue?.text = String.format(
            activity?.getString(R.string.rupees_value_double) ?: "",
            cartData?.final_amount
        )
        tvCheckoutPrice?.text = tvTotalAmountValue?.text.toString()
        isRiggleCoinApplied = false
        /*tvRiggleDiscountValue?.text =
            String.format(activity?.getString(R.string.rupees_value) ?: "", 0)*/
    }

    override fun itemClicked(
        product_id: Int,
        itemCount: String,
        productsData: ProductsData,
        type: Int
    ) {
        var itemCount = itemCount
        showHideLoader(true)
        //worst case scenario, in case the value goes into negative due to some bug in calculation, user was not able to remove the item from cart at all.
        if (itemCount.toInt() < 0) {
            itemCount = "0"
        }
        val variantUpdate = if (type == 2) VariantUpdate(null, itemCount.toInt(), product_id)
        else VariantUpdate(product_id, itemCount.toInt(), null)
        val item = ArrayList<VariantUpdate>()
        item.add(variantUpdate)
        //val cartDataRequest = RequestCartData(item)
        val cartRequest = ProductCartRequest(item)
        userPreference.userData?.retailer?.id?.let {
            showHideLoader(true)
            dataManager.addCartItems(object :
                ApiResponseListener<List<APICommonResponse<ProductsData>>> {
                override fun onSuccess(response: List<APICommonResponse<ProductsData>>) {
                    if (response != null) {
                        /*response.data?.current_object?.let {
                            itemUpdated(it)
                        }*/

                    }
                    fetchCart()
                    showHideLoader(false)
                }

                override fun onError(apiError: ApiError?) {
                    showHideLoader(false)
                    if (!apiError?.message.equals("Parsing error", false))
                        Toast.makeText(
                            activity,
                            apiError?.message,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                }
            }, it, cartRequest)
        }

        /*dataManager.editCart(object : ApiResponseListener<APICommonResponse<EditCartResponse>> {
            override fun onSuccess(response: APICommonResponse<EditCartResponse>) {
                showHideLoader(false)

                //if cart List is empty hide the cartView
                cartAdapter?.let {
                    if (it.productsData.isEmpty()) {
                        showEmptyCartView()
                        tvCartPrice?.text =
                            String.format(
                                activity?.getString(R.string.rupees_value_double) ?: "",
                                0.0
                            )
                        tvDiscountValue?.text =
                            String.format(
                                activity?.getString(R.string.rupees_value_double) ?: "",
                                0.0
                            )
                        tvTotalAmountValue?.text =
                            String.format(
                                activity?.getString(R.string.rupees_value_double) ?: "",
                                0.0
                            )
                        cartLinearLayout?.visibility = View.GONE
                    } else {
                        hideEmptyCartView()
                        response?.data?.cart_details?.let {
                            //cartData?.cart_details = it
                        }
                        if (isRiggleCoinApplied) {
                            applyRiggleCoin()
                        }
                        //populateCartDetails(response?.data?.cart_details)
                    }
                    response?.data?.parent_object?.let {
                        val updateProdOnHome = UpdateProdOnHome(it)
                        GlobalBus.bus?.post(updateProdOnHome)
                    }

                }

            }

            override fun onError(apiError: ApiError?) {
                showHideLoader(false)
            }
        }, cartDataRequest)*/
    }

    private fun updateCartItem(data: EditCartResponse?) {
        /*cartData?.let { cartData ->
            if (data != null && data.cart_details != null) {
                cartData.cart_details = data.cart_details
                if (data.current_object.item_cart == 0) {
                    for (i in cartData.products_in_cart.indices) {
                        if (data.current_object.id == cartData.products_in_cart[i].id) cartData.products_in_cart.removeAt(
                            i
                        )
                        cartAdapter?.removeItemFromCart(i)
                    }
                }
                //populateCartDetails(response.getData().getCart_details());
                val updateProdOnHome = UpdateProdOnHome(data.parent_object)
                GlobalBus.bus?.post(updateProdOnHome)
            }
        }*/

    }

    private fun showHideLoader(state: Boolean) {
        if (loaderDialog != null) {
            /*if (!(activity as Activity).isFinishing)*/activity?.let {
                if (state) loaderDialog?.show() else loaderDialog?.dismiss()
            }
        } else {
            activity?.let {
                loaderDialog = LoadingDialog(activity)
                showHideLoader(state)
            }

        }
    }

    fun clearCart() {
        activity?.let { activity ->
            cartAdapter = CartAdapter(activity, ArrayList())
            tvCartPrice?.text = String.format(activity.getString(R.string.rupees_value_double), 0.0)
            tvDiscountValue?.text =
                String.format(activity.getString(R.string.rupees_value_double), 0.0)
            tvTotalAmountValue?.text =
                String.format(activity.getString(R.string.rupees_value_double), 0.0)
            cartLinearLayout?.visibility = View.GONE
            cbRiggleCoins.isChecked = false

            showEmptyCartView()
        }
    }

    override fun onResume() {
        super.onResume()
        getDetails()
    }

    private fun getDetails() {
        userPreference.userData?.retailer?.id?.let {
            dataManager.getPingDetails(
                object :
                    ApiResponseListener<JsonElement> {
                    override fun onSuccess(response: JsonElement) {
                        response?.let {
                            var usrData =
                                Gson().fromJson(it.toString(), UserDetails::class.java)
                            if (usrData.riggle_coins_balance != 0) {
                                showAvailableCoinsView()
                                tvAvailableCoins?.hint = String.format(
                                    activity?.getString(R.string.available_coins_value) ?: "",
                                    usrData.riggle_coins_balance
                                )
                                avail_riggle_coin = usrData.riggle_coins_balance
                            } else {
                                hideAvailableCoinsView()
                            }
                        }
                    }

                    override fun onError(apiError: ApiError?) {
                        Log.i("TAG", "::::" + apiError?.message)
                    }
                },
                it, ""
            )
            //expand = sub_area
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): CartFragment {
            val fragment = CartFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}