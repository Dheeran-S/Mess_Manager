package com.theayushyadav11.MessEase.ui.NavigationDrawers.Fragments


import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.SetOptions
import com.theayushyadav11.MessEase.Models.Comment
import com.theayushyadav11.MessEase.Models.Menu
import com.theayushyadav11.MessEase.Models.Msg
import com.theayushyadav11.MessEase.Models.OptionSelected
import com.theayushyadav11.MessEase.Models.Poll
import com.theayushyadav11.MessEase.R
import com.theayushyadav11.MessEase.RoomDatabase.MenuDataBase.MenuDatabase
import com.theayushyadav11.MessEase.databinding.FragmentHomeBinding
import com.theayushyadav11.MessEase.ui.Adapters.CommentAdapter
import com.theayushyadav11.MessEase.ui.Adapters.DateAdapter
import com.theayushyadav11.MessEase.ui.Adapters.DateItem
import com.theayushyadav11.MessEase.ui.Adapters.MsgAdapter
import com.theayushyadav11.MessEase.ui.NavigationDrawers.ViewModels.HomeViewModel
import com.theayushyadav11.MessEase.ui.NavigationDrawers.ViewModels.MessLeavesViewModel
import com.theayushyadav11.MessEase.ui.NavigationDrawers.viewModelFactories.HomeViewModelFactory
import com.theayushyadav11.MessEase.utils.Constants.Companion.auth
import com.theayushyadav11.MessEase.utils.Constants.Companion.databaseReference
import com.theayushyadav11.MessEase.utils.Constants.Companion.fireBase
import com.theayushyadav11.MessEase.utils.Constants.Companion.firestoreReference
import com.theayushyadav11.MessEase.utils.Constants.Companion.getCurrentDate
import com.theayushyadav11.MessEase.utils.Constants.Companion.getCurrentTimeInAmPm
import com.theayushyadav11.MessEase.utils.Mess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), DateAdapter.Listeners {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mess: Mess
    private val homeViewModel: HomeViewModel by viewModels {
        val database = MenuDatabase.getDatabase(requireActivity())
        HomeViewModelFactory(database.menuDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            mess = Mess(requireContext())
            mess.log("HomeFragment onViewCreated started")
            setDateAdapter()
            initialise()
            
            // Load leave status for user
            setupLeaveStatus()
            
            // NOW ENABLE EVERYTHING with safety checks
            setMainMenu {
                mess.log("setMainMenu completed successfully")
                updateDetails()
                listeners()
                updateUI()  // NOW ENABLED with ViewModel safety checks
            }
            
            mess.log("HomeFragment onViewCreated completed")
        } catch (e: Exception) {
            mess.log("CRASH IN onViewCreated: ${e.message}")
            e.printStackTrace()
            mess.pbDismiss()
        }
    }
    
    private fun setupLeaveStatus() {
        try {
            val user = mess.getUser()
            if (user.uid.isEmpty()) {
                // Hide the leave card if user not logged in
                binding.root.findViewById<View>(R.id.leaveStatusCard)?.visibility = View.GONE
                return
            }
            
            val leavesViewModel = ViewModelProvider(this)[MessLeavesViewModel::class.java]
            
            // Get user's leaves
            leavesViewModel.getUserLeaves(user.uid) { leaves ->
                val leaveCard = binding.root.findViewById<View>(R.id.leaveStatusCard)
                
                if (leaves.isEmpty()) {
                    leaveCard?.visibility = View.GONE
                    return@getUserLeaves
                }
                
                leaveCard?.visibility = View.VISIBLE
                
                // Count by status (include both DENIED and REJECTED)
                val pendingCount = leaves.count { it.status == "PENDING_APPROVAL" }
                val approvedCount = leaves.count { it.status == "APPROVED" }
                val deniedCount = leaves.count { it.status == "DENIED" || it.status == "REJECTED" }
                
                // Update UI
                leaveCard?.findViewById<TextView>(R.id.tvPendingCount)?.text = pendingCount.toString()
                leaveCard?.findViewById<TextView>(R.id.tvApprovedCount)?.text = approvedCount.toString()
                leaveCard?.findViewById<TextView>(R.id.tvDeniedCount)?.text = deniedCount.toString()
                
                // Show latest leave
                val latestLeave = leaves.firstOrNull()
                if (latestLeave != null) {
                    val latestContainer = leaveCard?.findViewById<LinearLayout>(R.id.latestLeaveContainer)
                    val tvLatestLeave = leaveCard?.findViewById<TextView>(R.id.tvLatestLeave)
                    
                    latestContainer?.visibility = View.VISIBLE
                    val leaveType = when (latestLeave.type) {
                        "FULL_DAY" -> "Full Day"
                        "MEAL_SKIP" -> "Meal Skip (${latestLeave.meal})"
                        "EMERGENCY" -> "Emergency"
                        else -> latestLeave.type
                    }
                    val statusText = when (latestLeave.status) {
                        "PENDING_APPROVAL" -> "Pending"
                        "APPROVED" -> "Approved"
                        "DENIED" -> "Denied"
                        else -> latestLeave.status
                    }
                    tvLatestLeave?.text = "$leaveType - ${latestLeave.date} - $statusText"
                }
                
                // View all button - navigate to Mess Leave page
                leaveCard?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewAllLeaves)?.setOnClickListener {
                    try {
                        val navController = findNavController()
                        navController.navigate(R.id.nav_mess_leave)
                    } catch (e: Exception) {
                        mess.log("Error navigating to mess leave: ${e.message}")
                        mess.toast("Unable to open leave page")
                    }
                }
            }
        } catch (e: Exception) {
            mess.log("Error setting up leave status: ${e.message}")
            e.printStackTrace()
            binding.root.findViewById<View>(R.id.leaveStatusCard)?.visibility = View.GONE
        }
    }

    //Initialising all the variables etc...
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initialise() {
        mess = Mess(requireContext())
        onRvScroll()
        binding.rv.smoothScrollToPosition(Date().date + 2)
        homeViewModel.monthYear.observe(viewLifecycleOwner) {
            binding.month.text = it
        }
        mess.addPb("Loading")
    }

    //Listeners for the onClicks
    private fun listeners() {
        binding.imageView2.setOnClickListener {
            scrollToPosition(1)
        }
        binding.imageView.setOnClickListener {
            scrollToPosition(-1)
        }
        binding.rr.setOnRefreshListener{
            updateUI()
            binding.rr.isRefreshing = false
        }
    }

    //Setting the Adapters
    private fun setDateAdapter() {


        val rv = binding.rv
        val adapter = DateAdapter(this)
        rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = adapter


    }

    //Handle the date selection
    override fun onDateSelected(date: DateItem, position: Int, main: DateAdapter.DateViewHolder) {
        homeViewModel.day.value = position + 1
        homeViewModel.dayOfWeek.value = date.weekday

        binding.rv.smoothScrollToPosition(position + 3)

    }

    //Handle the scroll of the recycler view
    fun onRvScroll() {
        val layoutManager = binding.rv.layoutManager as LinearLayoutManager
        binding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()


                if (firstVisibleItemPosition > 6) {
                    binding.imageView2.visibility = View.VISIBLE
                }
                if (lastVisibleItemPosition < 24) {
                    binding.imageView.visibility = View.VISIBLE
                }
            }
        })
    }

    //Scroll to the position in date
    private fun scrollToPosition(direction: Int) {
        val layoutManager = binding.rv.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        if (direction == -1) {
            if (firstVisibleItemPosition > 4) {
                binding.imageView2.visibility = View.VISIBLE
                binding.rv.smoothScrollToPosition(firstVisibleItemPosition - 4)

            } else {
                binding.rv.smoothScrollToPosition(0)
                binding.imageView.visibility = View.INVISIBLE
            }
        } else if (direction == 1) {
            if (lastVisibleItemPosition < 29) {
                binding.rv.smoothScrollToPosition(lastVisibleItemPosition + 4)

                binding.imageView.visibility = View.VISIBLE
            } else {
                binding.imageView2.visibility = View.INVISIBLE
            }
        }
    }

    fun updateUI() {
        try {
            homeViewModel.day.observe(requireActivity(), Observer {
                //Adding menu
                addFood()
                //Adding poll
                addPolls(it)
                //Adding Msgs
                addMsgs(it)
            })
        } catch (e: Exception) {
            mess.log("Error in updateUI: ${e.message}")
            e.printStackTrace()
        }
    }

    fun setAdapters() {
        if (isAdded) {
            homeViewModel.day.observe(requireActivity(), Observer {
                val user = mess.getUser()
                if (isAdded) {
                    homeViewModel.getDayMsgs(user, getDate(it)) { msgs ->
                        setMsgAdapter(msgs)
                    }
                }

            })
        }
    }

    private fun setMsgAdapter(msgs: List<Msg>) {
        if (isAdded) {
            val adapter = MsgAdapter(msgs, requireContext())
            binding.rvMsg.layoutManager = mess.getmanager()
            binding.rvMsg.adapter = adapter
        }

    }


    private fun addMsgs(day: Int) {
        try {
            val user = mess.getUser()
            if (user.uid.isEmpty()) {
                mess.log("User UID is empty, skipping messages load")
                binding.msgAdder.removeAllViews()
                return
            }
            homeViewModel.getDayMsgs(user, getDate(day)) { msgs ->
                binding.msgAdder.removeAllViews()
                if (msgs.isNotEmpty()) {
                    msgs.forEach {
                        addMsg(it)
                    }
                }
            }
        } catch (e: Exception) {
            mess.log("Error loading messages: ${e.message}")
            e.printStackTrace()
            binding.msgAdder.removeAllViews()
        }
    }

    private fun addMsg(msg: Msg) {
        if (isAdded) {
            val msgLayout = LayoutInflater.from(requireActivity())
                .inflate(R.layout.msg_element_layout, binding.msgAdder, false)
            msgLayout.findViewById<TextView>(R.id.title).text = msg.title
            msgLayout.findViewById<TextView>(R.id.creater).text = msg.creater.name
            msgLayout.findViewById<TextView>(R.id.time).text = msg.time
            msgLayout.findViewById<TextView>(R.id.body).text = msg.body
            msgLayout.findViewById<TextView>(R.id.date).text = msg.date
            msgLayout.findViewById<ImageView>(R.id.dIcon)
                .setImageResource(fireBase.getIcon(msg.creater.designation))
            msgLayout.findViewById<LinearLayout>(R.id.comments).setOnClickListener {
                showBottomSheetDialog(msg)
            }

            val adder = msgLayout.findViewById<ViewGroup>(R.id.adder)
            for (photo in msg.photos) {
                val view = LayoutInflater.from(context).inflate(R.layout.img, adder, false)
                val img = view.findViewById<ImageView>(R.id.img)
                mess.loadImage(photo, img)
                img.setOnClickListener {
                    mess.showImage(photo)
                }
                adder.addView(view)
            }
            binding.msgAdder.addView(msgLayout)
        }
    }


    private fun addFood() {
        try {
            homeViewModel.dayOfWeek.observe(requireActivity(), Observer { day ->
                try {
                    homeViewModel.getDayParticulars(requireContext(), day) { particulars ->
                        if (isAdded) {
                            binding.menuAdder.removeAllViews()
                            if (particulars.isNotEmpty()) {
                                particulars.forEachIndexed { index, particular ->
                                    try {
                                        val menuLayout = LayoutInflater.from(requireActivity()).inflate(
                                            R.layout.particulars_element_layout, binding.menuAdder, false
                                        )

                                        val foodType = menuLayout.findViewById<TextView>(R.id.foodType)
                                        val foodMenu = menuLayout.findViewById<TextView>(R.id.foodMenu)
                                        val foodTimimg = menuLayout.findViewById<TextView>(R.id.foodTimeing)
                                        val view = menuLayout.findViewById<View>(R.id.view3)
                                        val card = menuLayout.findViewById<MaterialCardView>(R.id.card)
                                        val clock = menuLayout.findViewById<ImageView>(R.id.imageView4)
                                        foodType.text = particular.type
                                        foodMenu.text = particular.food
                                        foodTimimg.text = particular.time
                                        homeViewModel.getSpecialMeal(
                                            homeViewModel.day.value ?: 1
                                        ) { meal, mealIndex ->
                                            if (meal.isNotEmpty() && mealIndex == index) {
                                                foodType.text = "Special ${particular.type}"
                                                foodMenu.text = meal
                                                foodTimimg.text = particular.time
                                                view.setBackgroundColor(requireContext().resources.getColor(R.color.gold))
                                                card.setCardBackgroundColor(
                                                    requireContext().resources.getColor(R.color.light_gold)
                                                )
                                                foodType.setTextColor(requireContext().resources.getColor(R.color.gold))
                                                foodMenu.setTextColor(Color.BLACK)
                                                foodTimimg.setTextColor(requireContext().resources.getColor(R.color.gold))
                                                clock.setImageResource(R.drawable.goldentime)
                                            }
                                        }
                                        binding.menuAdder.addView(menuLayout)
                                    } catch (e: Exception) {
                                        mess.log("Error adding food item: ${e.message}")
                                    }
                                }
                            }
                            mess.pbDismiss()
                        }
                    }
                } catch (e: Exception) {
                    mess.log("Error in getDayParticulars: ${e.message}")
                    e.printStackTrace()
                    mess.pbDismiss()
                }
            })
        } catch (e: Exception) {
            mess.log("Error in addFood: ${e.message}")
            e.printStackTrace()
            mess.pbDismiss()
        }
    }

    private fun addPolls(day: Int) {
        try {
            val user = mess.getUser()
            if (user.uid.isEmpty()) {
                mess.log("User UID is empty, skipping polls load")
                binding.pollAdder.removeAllViews()
                return
            }
            homeViewModel.getDayPolls(user, getDate(day)) { polls ->
                binding.pollAdder.removeAllViews()
                if (polls.isNotEmpty()) {
                    binding.pollAdder.removeAllViews()
                    polls.forEach {
                        addPoll(it)
                    }
                }
            }
        } catch (e: Exception) {
            mess.log("Error loading polls: ${e.message}")
            e.printStackTrace()
            binding.pollAdder.removeAllViews()
        }
    }

    private fun addPoll(poll: Poll) {
        if (isAdded) {
            val pollLayout = LayoutInflater.from(requireActivity())
                .inflate(R.layout.poll_element_layout, binding.pollAdder, false)
            pollLayout.findViewById<TextView>(R.id.tvQuestion).text = poll.question
            pollLayout.findViewById<TextView>(R.id.tvname).text = poll.creater.name
            pollLayout.findViewById<TextView>(R.id.time).text = poll.time
            pollLayout.findViewById<TextView>(R.id.date).text = poll.date
            pollLayout.findViewById<ImageView>(R.id.dIcon)
                .setImageResource(fireBase.getIcon(poll.creater.designation))
            pollLayout.findViewById<LinearLayout>(R.id.vv).visibility = View.GONE
            pollLayout.findViewById<ImageView>(R.id.delete).visibility = View.GONE
            addOptions(poll, pollLayout.findViewById(R.id.radioGroup))
            binding.pollAdder.addView(pollLayout)
        }

    }

    private fun addOptions(poll: Poll, adder: LinearLayout) {
        val listOfRb = mutableListOf<RadioButton>()
        poll.options.forEach { option ->
            val optionLayout =
                LayoutInflater.from(requireActivity()).inflate(R.layout.option_layout, adder, false)
            val rb = optionLayout.findViewById<RadioButton>(R.id.rb)
            val optionTitle = optionLayout.findViewById<TextView>(R.id.title)
            val optionVotes = optionLayout.findViewById<TextView>(R.id.nop)
            optionTitle.text = option
            val pb = optionLayout.findViewById<ProgressBar>(R.id.ProgressBar)
            homeViewModel.getTotalVotes(poll.id) { nov ->

                homeViewModel.getVotesOnOption(poll.id, option) {

                    val v = if (nov == 0) 0 else (it * 100) / nov
                    pb.progress = v


                    optionVotes.text = it.toString()
                }
            }

            listOfRb.add(rb)
            homeViewModel.getVoteByUid(poll.id) {
                if (option == it) {
                    rb.isChecked = true
                }
            }

            optionLayout.setOnClickListener {

                onOptionClicked(poll.id, listOfRb, poll.options.indexOf(option), poll.options)
            }

            rb.setOnClickListener {
                onOptionClicked(poll.id, listOfRb, poll.options.indexOf(option), poll.options)
            }

            adder.addView(optionLayout)
        }
    }

    private fun onOptionClicked(
        pid: String,
        listOfRb: MutableList<RadioButton>,
        currentIndex: Int,
        options: MutableList<String>
    ) {
        for (i in 0 until listOfRb.size) {
            if (i == currentIndex) {
                listOfRb[i].isChecked = true
                optionSelect(pid, options[i])

            } else {
                listOfRb[i].isChecked = false

            }
        }
    }

    private fun optionSelect(pid: String, option: String) {
        try {
            val user = mess.getUser()
            if (user.uid.isEmpty()) {
                mess.log("User UID is empty, cannot select poll option")
                mess.toast("Please complete your profile to vote")
                return
            }
            val optionSelected = OptionSelected(
                user = user, selected = option, time = getCurrentTimeInAmPm(), date = getCurrentDate()
            )
            homeViewModel.selectOption(pid, optionSelected)
        } catch (e: Exception) {
            mess.log("Error selecting poll option: ${e.message}")
            e.printStackTrace()
        }
    }

    fun updateDetails() {
        val name: String = auth.currentUser?.displayName.toString()
        val photoUrl = auth.currentUser?.photoUrl
        val detailMap = mutableMapOf<String, Any>()
        if (name != "" && name != "null") detailMap["name"] = name
        if (photoUrl != null) detailMap["photoUrl"] = photoUrl.toString()
        firestoreReference.collection("Users").document(auth.currentUser?.uid.toString())
            .set(detailMap, SetOptions.merge())
    }


    fun getDate(day: Int): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("${day}/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun showBottomSheetDialog(msg: Msg) {
        try {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetView = layoutInflater.inflate(R.layout.comments_layout, binding.root, false)
            bottomSheetDialog.setContentView(bottomSheetView)

            val dismiss = bottomSheetDialog.findViewById<ImageView>(R.id.dismiss)
            dismiss?.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            val user = mess.getUser()
            if (isAdded && user.photoUrl.isNotEmpty()) {
                mess.loadCircularImage(
                    user.photoUrl, bottomSheetDialog.findViewById<ImageView>(R.id.profileIcon)!!
                )
            }

            val rv = bottomSheetDialog.findViewById<RecyclerView>(R.id.rv)
            val message = bottomSheetDialog.findViewById<TextView>(R.id.message)!!
            if (rv != null) {
                setAdapter(msg, rv, message)
            }
            val send = bottomSheetDialog.findViewById<ImageView>(R.id.send)
            send?.setOnClickListener {
                val commentText = bottomSheetDialog.findViewById<EditText>(R.id.etComment)

                if (commentText != null) {
                    addComment(commentText, msg)
                }
            }
            bottomSheetDialog.show()
        } catch (e: Exception) {
            mess.log("Error showing bottom sheet: ${e.message}")
            e.printStackTrace()
        }
    }

    fun addComment(editText: EditText, msg: Msg) {
        try {
            val commentText = editText.text.toString()
            if (commentText.isNotEmpty()) {
                val key = databaseReference.push().key.toString()

                val user = mess.getUser()
                if (user.uid.isEmpty()) {
                    mess.toast("Please complete your profile to comment")
                    return
                }

                val comment = Comment(
                    id = key,
                    comment = commentText,
                    time = getCurrentTimeInAmPm(),
                    date = getCurrentDate(),
                    creator = user,
                )

                firestoreReference.collection("Msgs").document(msg.uid).collection("Comments")
                    .document(key).set(comment).addOnCompleteListener {
                        if (it.isSuccessful) {
                            mess.toast("Comment added")
                            editText.text = null
                        } else {
                            mess.toast(it.exception?.message.toString())
                        }
                    }
            }
        } catch (e: Exception) {
            mess.log("Error adding comment: ${e.message}")
            e.printStackTrace()
            mess.toast("Error adding comment")
        }
    }

    private fun setAdapter(msg: Msg, rv: RecyclerView, m: TextView) {
        homeViewModel.getComments(msg.uid) { comments ->
            mess.log(comments)
            if (comments.isEmpty()) {
                m.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                rv.visibility = View.VISIBLE
                m.visibility = View.GONE
                if (isAdded) {
                    rv.layoutManager = LinearLayoutManager(context)
                    val adapter = CommentAdapter(comments, requireContext(), msg.uid)
                    rv.adapter = adapter
                }

            }
        }
    }

    private fun setMainMenu(onResult: () -> Unit) {
        firestoreReference.collection("MainMenu").document("menu")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    mess.log("Error fetching main menu: ${error.message}")
                    onResult()
                    return@addSnapshotListener
                }
                if (value != null && value.exists()) {
                    try {
                        val menu = value.toObject(Menu::class.java)
                        if (menu != null) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val menuDatabase = MenuDatabase.getDatabase(requireContext()).menuDao()
                                val newMenu = Menu(
                                    id = 0, creator = menu.creator, menu = menu.menu
                                )
                                menuDatabase.addMenu(newMenu)
                                withContext(Dispatchers.Main) {
                                    onResult()
                                }
                            }
                        } else {
                            mess.log("Menu object is null")
                            onResult()
                        }
                    } catch (e: Exception) {
                        mess.log("Error parsing menu: ${e.message}")
                        onResult()
                    }
                } else {
                    mess.log("Menu document doesn't exist")
                    onResult()
                }
            }

    }
}

