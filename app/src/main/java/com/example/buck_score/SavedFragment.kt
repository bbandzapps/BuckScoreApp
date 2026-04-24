package com.example.buck_score
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class SavedFragment: Fragment(R.layout.fragment_saved) {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScorecardAdapter
    private lateinit var dao: ScorecardDao

    private lateinit var emptySelectedText: TextView
    private lateinit var selectedContent: LinearLayout

    private lateinit var deleteBtn: Button
    private lateinit var openBtn: Button
    
    private lateinit var selectedName: TextView
    private lateinit var selectedDetails: TextView
    private lateinit var selectedDate: TextView
    private lateinit var selectedScore: TextView
    private lateinit var selectedImage: ImageView
    private lateinit var noImageText: TextView
    

    private var query: String=""
    private lateinit var searchBar: EditText
    private lateinit var filterBox: Spinner
    val filters = arrayOf(
        "No Filter",
        "Date" /*Needs before/after with date input - calendar?*/,
        "Score" /*Needs higher/lower with score input - need spinner?*/,
        "Species" /*Needs dropdown for species input*/
    )
    private lateinit var filterContainer: LinearLayout
    private var minScore: Double? = null
    private var maxScore: Double? = null
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var selectedFilterSpecies: String? = null

    private lateinit var sortBox: Spinner
    val sorts = arrayOf(
        "Newest", "Oldest",
        "Name A–Z", "Name Z–A",
        "Species A–Z", "Species Z–A",
        "Score High–Low", "Score Low–High"
    )

    private var searchJob: Job? = null

    private lateinit var root: View
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root = view

        emptySelectedText = root.findViewById(R.id.emptySelectedText)
        selectedContent = root.findViewById(R.id.selectedContent)

        selectedName = root.findViewById(R.id.selectedName)
        selectedDetails = root.findViewById(R.id.selectedDetails)
        selectedScore = root.findViewById(R.id.selectedScore)
        selectedDate = root.findViewById(R.id.selectedDate)
        selectedImage = root.findViewById(R.id.selectedImage)
        noImageText = root.findViewById(R.id.noImageText)
        deleteBtn = root.findViewById(R.id.deleteBtn)
        openBtn = root.findViewById(R.id.openBtn)

        searchBar = root.findViewById(R.id.searchBox)
        filterBox = root.findViewById(R.id.filterBox)
        filterContainer = root.findViewById(R.id.filterContainer)
        sortBox = root.findViewById(R.id.sortBox)

        recyclerView = view.findViewById(R.id.scorecardRecycler)

        adapter = ScorecardAdapter(emptyList()) { selected ->
            displaySelectedCard(selected)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        dao = (requireActivity().application as BuckScoreApp)
            .database
            .scorecardDao()

        loadSpinner(sortBox, sorts)
        loadSpinner(filterBox, filters)
        loadScorecards()

        deleteBtn.setOnClickListener {
            val selected = adapter.getSelectedItem() ?: return@setOnClickListener

            AlertDialog.Builder(requireContext())
                .setTitle("Delete Scorecard")
                .setMessage("Are you sure you want to delete this Scorecard?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        dao.delete(selected)
                        loadScorecards()
                        showEmptyState()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ===============================
    // LOADING SCORECARDS
    // ===============================
    private fun loadScorecards() {
        viewLifecycleOwner.lifecycleScope.launch {
            val scorecards = dao.getAll() // assuming you have this
            adapter.updateList(scorecards)
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    delay(300) // wait for typing to pause
                    query = s.toString()
                    refreshList()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        filterBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterContainer.removeAllViews()
                clearFilters()
                refreshList()

                when (position) {
                    0 -> { // No Filter → do nothing
                    }

                    1 -> { // Date
                        val view = layoutInflater.inflate(R.layout.filter_date, filterContainer, false)
                        setupDateFilter(view)
                        filterContainer.addView(view)
                    }

                    2 -> { // Score
                        val view = layoutInflater.inflate(R.layout.filter_score, filterContainer, false)
                        setupScoreFilter(view)
                        filterContainer.addView(view)
                    }

                    3 -> { // Species
                        val view = layoutInflater.inflate(R.layout.filter_species, filterContainer, false)
                        setupSpeciesFilter(view)
                        filterContainer.addView(view)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        sortBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshList()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

    }

    private fun refreshList() {
        lifecycleScope.launch {
            val base = dao.getFiltered(
                query,
                minScore,
                maxScore,
                startDate,
                endDate,
                selectedFilterSpecies
            )

            val finalList = applySort(base)
            adapter.updateList(finalList)
        }
    }

    // ===============================
    // SORT METHODS
    // ===============================
    fun applySort(list: List<ScorecardEntity>): List<ScorecardEntity> {
        return when (sortBox.selectedItemPosition) {
            0 -> list.sortedByDescending { it.dateSaved }
            1 -> list.sortedBy { it.dateSaved }
            2 -> list.sortedBy { it.name.lowercase() }
            3 -> list.sortedByDescending { it.name.lowercase() }
            4 -> list.sortedBy { it.species.lowercase() }
            5 -> list.sortedByDescending { it.species.lowercase() }
            6 -> list.sortedByDescending { it.netScore }
            7 -> list.sortedBy { it.netScore }
            else -> list
        }
    }

    // ===============================
    // FILTER METHODS
    // ===============================
    private fun setupDateFilter(view: View) {
        val start = view.findViewById<EditText>(R.id.dateStart)
        val end = view.findViewById<EditText>(R.id.dateEnd)

        start.setOnClickListener {
            showDatePicker { timestamp ->
                start.setText(adapter.formatDate(timestamp))
                startDate = convertDateToMillis(start.text.toString())
                refreshList()
            }
        }

        end.setOnClickListener {
            showDatePicker { timestamp ->
                end.setText(adapter.formatDate(timestamp))
                endDate = convertDateToMillis(end.text.toString(), endOfDay = true)
                refreshList()
            }
        }
    }

    private fun setupScoreFilter(view: View){
        val min = view.findViewById<EditText>(R.id.minScore)
        val max = view.findViewById<EditText>(R.id.maxScore)

        min.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                minScore = s.toString().toDoubleOrNull()
                refreshList()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        max.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                maxScore = s.toString().toDoubleOrNull()
                refreshList()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    private fun setupSpeciesFilter(view: View){
        val speciesNames = Species.values().map { it.displayName }.toTypedArray()
        val spinner = view.findViewById<Spinner>(R.id.speciesFilterSpinner)
        loadSpinner(spinner, speciesNames)

        val speciesList = Species.values().toList()
        val names = speciesList.map { it.displayName }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_label,
            names
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_fraction)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedFilterSpecies = speciesList[position].displayName
                refreshList()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedFilterSpecies = null
                refreshList()
            }
        }
    }

    private fun clearFilters(){
        startDate = null
        endDate = null
        minScore = null
        maxScore = null
        selectedFilterSpecies = null
    }

    // ===============================
    // MISCELLANEOUS
    // ===============================

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day)
                onDateSelected(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadSpinner(spinner: Spinner, values : Array<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_fraction,
            values
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_fraction)
        spinner.adapter = adapter
    }

    fun convertDateToMillis(dateString: String, endOfDay: Boolean = false): Long? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val localDate = LocalDate.parse(dateString, formatter)

            val dateTime = if (endOfDay) {
                localDate.atTime(LocalTime.MAX) // 23:59:59.999999999
            } else {
                localDate.atStartOfDay()
            }

            dateTime.atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        } catch (e: Exception) {
            null
        }
    }

    fun displaySelectedCard(card: ScorecardEntity) {
        showSelectedState()
        selectedName.text = card.name
        //selectedDetails.text = "${card.species} | ${card.buckType ?: ""}"
        selectedDetails.text = buildString {
            append(card.species)
            card.buckType?.let { append(" | $it") }
        }

        selectedScore.text = "Score: ${doubleToBC(card.netScore)}"
        selectedDate.text = "Saved: ${adapter.formatDate(card.dateSaved)}"

        if (card.imagePath != null) {
            val file = File(card.imagePath)

            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                val scaled = scaleBitmap(bitmap, 500, 500) // reuse your function

                selectedImage.setImageBitmap(scaled)
                selectedImage.visibility = View.VISIBLE
                noImageText.visibility = View.GONE
            } else {
                selectedImage.visibility = View.GONE
                noImageText.visibility = View.VISIBLE
            }
        } else {
            selectedImage.visibility = View.GONE
            noImageText.visibility = View.VISIBLE
        }
    }

    fun showEmptyState() {
        emptySelectedText.visibility = View.VISIBLE
        selectedContent.visibility = View.GONE
        adapter.clearSelection()
    }

    fun showSelectedState() {
        emptySelectedText.visibility = View.GONE
        selectedContent.visibility = View.VISIBLE
    }



}