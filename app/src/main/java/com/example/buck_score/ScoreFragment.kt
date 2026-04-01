package com.example.buck_score

import androidx.fragment.app.Fragment


import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import kotlin.math.roundToInt
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.*
import android.graphics.Color
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.text.font.Typeface
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ================
// TABLE OF CONTENTS:
// Enums
// Data Classes
// Class Variables
// OnViewCreated
// SPECIES SELECTION
// SPECIES SETUP
// SETUP UI
// SETUP ANIMALS
// SWITCH HANDLING
// REGISTRATION / FIELD LISTENERS
// SCORING LOGIC
// PDF Download Functionality
// Clearing / Restoring
// SHOW SECTION METHODS
// MISCELLANEOUS HELPERS
// ================




class ScoreFragment : Fragment(R.layout.fragment_score) {


    // ===============================
    // Enums
    // ===============================
    enum class BuckType {
        TYPICAL,
        NONTYPICAL
    }

    enum class Species(val displayName: String) {
        WHITETAIL("Whitetail Deer"),
        COUES("Coues Deer"),
        SITKA_BLACKTAIL("Sitka Blacktail Deer"),
        COLUMBIA_BLACKTAIL("Colombia Blacktail Deer"),
        MULE_DEER("Mule Deer"),

        ROCKY_MOUNTAIN_ELK("Rocky Mountain Elk"),
        ROOSEVELT_ELK("Roosevelt Elk"),
        TULE_ELK("Tule Elk"),

        YUKON_MOOSE("Alaska-Yukon Moose"),
        CANADA_MOOSE("Canada Moose"),
        SHIRAS_MOOSE("Shiras Moose"),

        BIGHORN_SHEEP("Bighorn Sheep"),
        DALL_SHEEP("Dall Sheep"),
        DESERT_SHEEP("Desert Sheep"),
        STONE_SHEEP("Stone Sheep"),

        BARREN_GROUND_CARIBOU("Barren Ground Caribou"),
        CC_BARREN_GROUND_CARIBOU("Central Canada Barren Ground Caribou"),
        MOUNTAIN_CARIBOU("Mountain Caribou"),
        QUEBEC_LABRADOR_CARIBOU("Quebec-Labrador Caribou"),
        WOODLAND_CARIBOU("Woodland Caribou"),

        MOUNTAIN_GOAT("Mountain Goat"),
        BISON("Bison"),
        MUSK_OX("Musk Ox"),
        PRONGHORN("Pronghorn")
    }

    enum class Section{
        TYPE,
        POINT_COUNT,
        SPREADS,
        ABNORMALS,
        LENGTHS,
        WIDTHS,
        CROWN_POINTS,
        CIRCUMFERENCES
    }

    enum class RowLayoutType {
        LEFT_RIGHT,          // addStandardRow
        SINGLE,              // addSingleMeasurementRow
        LEFT_RIGHT_NO_FRAC,  // addNoFracRow
        LOC                  // addLocRow
    }

    // ===============================
    // Data classes
    // ===============================

    data class MeasurementField(
        val type: MeasurementType,
        val side: Side?,
        val inchesField: EditText,
        val fractionField: Spinner? = null
    ) {
        fun value(): Double {
            val whole = inchesField.text.toString().toIntOrNull() ?: 0
            val fraction: Double
            if(fractionField == null)
                fraction = 0.0
            else
                when(type) {
                    is MeasurementType.Loc -> fraction = getFractionValue(fractionField, 32)
                    //fraction = fractionField.selectedItemPosition / 8.0
                    else -> fraction = getFractionValue(fractionField, 8)
                }

            return whole + fraction
        }
        fun getFractionValue(spinner: Spinner, denominator: Int): Double {
            return spinner.selectedItemPosition.toDouble() / denominator
        }
    }

    data class PairedMeasurement(
        val type: MeasurementType,
        val left: Double,
        val right: Double
    ) {
        fun difference(): Double = kotlin.math.abs(left - right)
        fun sum(): Double = left + right
    }

    sealed class MeasurementType {
        data class G(val index: Int) : MeasurementType()
        data class AbnormalPoint(val index: Int) : MeasurementType()
        object MainBeam : MeasurementType()
        data class Circumference(val index: Int) : MeasurementType()
        object InnerSpread : MeasurementType()
        object TipSpread : MeasurementType()
        object GreatestSpread : MeasurementType()
        object PointCount : MeasurementType()
        object BrowPointCount : MeasurementType()
        object Width: MeasurementType()
        object BrowWidth: MeasurementType()
        object ProngLength: MeasurementType()
        data class CrownPoint(val index: Int) : MeasurementType()
        data class Loc(val index: Int) : MeasurementType()
    }

    enum class Side { LEFT, RIGHT }

    data class ScoreBreakdown(
        val leftSum: Double,
        val rightSum: Double,
        val differenceTotal: Double,
        val abnormalSum: Double,
        val spreadCredit: Double,
        val subtotal: Double,
        val crownPointScore: Double,
        val gross: Double,
        val finalScore: Double,
        val spreadIsCapped: Boolean = false
    )

    data class MeasurementValue(
        val type: MeasurementType,
        val side: Side?,
        val value: Double
    )


    class MeasurementStore {

        private val values = mutableListOf<MeasurementValue>()

        fun update(field: MeasurementField) {
            val newVal = MeasurementValue(
                field.type,
                field.side,
                field.value()
            )

            values.removeAll { it.type == newVal.type && it.side == newVal.side }
            values.add(newVal)
        }

        fun get(type: MeasurementType, side: Side? = null): Double {
            return values.find { it.type == type && it.side == side }?.value ?: 0.0
        }

        fun getPaired(type: MeasurementType): PairedMeasurement {
            val left = get(type, Side.LEFT)
            val right = get(type, Side.RIGHT)
            return PairedMeasurement(type, left, right)
        }

//        fun getPairedList(filter: (MeasurementType) -> Boolean): List<PairedMeasurement> {
//            return values
//                .map { it.type }
//                .filter(filter)
//                .distinct()
//                .map { getPaired(it) }
//        }
        fun getPairedList(
            predicate: (MeasurementType) -> Boolean
        ): List<PairedMeasurement> =
            values
                .mapNotNull { it.type.takeIf(predicate) }
                .distinctBy { it } // still fine because types are unique per index
                .sortedBy {
                    when (it) {
                        is MeasurementType.Circumference -> it.index
                        is MeasurementType.G -> it.index
                        is MeasurementType.AbnormalPoint -> it.index
                        is MeasurementType.CrownPoint -> it.index
                        else -> 0
                    }
                }
                .map { getPaired(it) }

        fun getAll(): List<MeasurementValue> = values
        fun clear(){
            values.clear()
        }
    }

    data class SectionConfig(
        val type: Section,
        val title: String,
        val note: String? = null,
        val subNote: String? = null,
        val rows: List<RowConfig>,
        val maxDynamicRows: Int = 0,
        val dynamicBaseType: MeasurementType? = null
    )

    data class RowConfig(
        val label: String,
        val type: MeasurementType,
        val layoutType: RowLayoutType,
        val showDifference: Boolean = true
    )

    data class ScoreDisplayConfig(
        val showSpread: Boolean = true,
        val showAbnormals: Boolean = true,
        val showCrownPointScore: Boolean = false
    )

    data class SectionView(
        val root: View,
        val container: LinearLayout,
        val title: TextView,
        val note: TextView,
        val subNote: TextView?,
        val addButton: Button?
    )


    // ===============================
    // Class Variables
    // ===============================
    private val measurementStore = MeasurementStore()
    //private val measurements = mutableListOf<MeasurementField>()
    private lateinit var sectionViews: Map<Section, SectionView>

    val eighthFractions = arrayOf(
        "0/8", "1/8", "2/8", "3/8", "4/8", "5/8", "6/8", "7/8"
    )

    val locationFractions = arrayOf(
        "0/32", "1/32", "1/16", "3/32", "1/8", "5/32", "3/16", "7/32",
        "2/8", "9/32", "5/16", "11/32", "3/8", "13/32", "7/16", "15/32",
        "4/8", "17/32", "9/16", "19/32", "5/8", "21/32", "11/16", "23/32",
        "6/8", "25/32", "13/16", "27/32", "7/8", "29/32", "15/16", "31/32"
    )


    private lateinit var spreadCreditLabel: TextView
    private lateinit var spreadLabel: TextView
    private lateinit var spreadCreditNote: TextView

    private lateinit var grossScoreText: TextView
    private lateinit var differenceText: TextView
    private lateinit var abnormalText: TextView
    private lateinit var spreadCreditText: TextView
    private lateinit var leftSumText: TextView
    private lateinit var rightSumText: TextView
    private lateinit var crownScoreText: TextView
    private lateinit var subtotalText: TextView
    private lateinit var finalScoreText: TextView

    private lateinit var animalName:EditText
    private lateinit var leftPoints:EditText
    private lateinit var rightPoints:EditText
    private lateinit var typicalSwitch: SwitchMaterial
    private var buckType = BuckType.TYPICAL
    private lateinit var typicalLabel:TextView
    private lateinit var nontypicalLabel:TextView

    private lateinit var tipSpreadInches:EditText
    private lateinit var tipSpreadFractions: AppCompatSpinner
    private lateinit var greatestSpreadInches:EditText
    private lateinit var greatestSpreadFractions: AppCompatSpinner

    private lateinit var photoContainer: FrameLayout
    private lateinit var buckImageView: ImageView
    private lateinit var addPhotoText: TextView

    private lateinit var downloadBtn: Button
    private lateinit var saveBtn: Button

    private lateinit var currentScore: ScoreBreakdown
    private var buckPic: Bitmap? = null
    private val PAGE_WIDTH = 612    // 8.5" * 72 dpi
    private val PAGE_HEIGHT = 792   // 11" * 72 dpi
    private val MARGIN = 40
    private val LINE_HEIGHT = 24

    private lateinit var root: View

    private var currentSpecies: Species = Species.WHITETAIL
    private var currentProfile: ScoringProfile = DeerProfile()
    private lateinit var speciesImage: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onViewCreated(view, savedInstanceState)
        root = view
        initSectionViews(view)

        val speciesToolbar = root.findViewById<MaterialToolbar>(R.id.speciesToolbar)

        // ============================
        // Bind ALWAYS-EXISTING VIEWS
        // ============================
        animalName = root.findViewById(R.id.animal_name)
        speciesImage = view.findViewById(R.id.speciesImage)

        grossScoreText = root.findViewById(R.id.grossScoreText)
        abnormalText = root.findViewById(R.id.abnormalText)
        differenceText = root.findViewById(R.id.differenceText)
        spreadCreditText = root.findViewById(R.id.spreadCreditText)
        leftSumText = root.findViewById(R.id.leftSumText)
        rightSumText = root.findViewById(R.id.rightSumText)
        crownScoreText = root.findViewById(R.id.crownPointSumText)
        subtotalText = root.findViewById(R.id.subtotalText)
        finalScoreText = root.findViewById(R.id.finalScoreText)

        spreadCreditLabel = root.findViewById(R.id.spreadCreditLabel)
        spreadLabel = root.findViewById(R.id.spreadLabel)
        spreadCreditNote = root.findViewById(R.id.spreadCreditNote)


        typicalLabel = root.findViewById<TextView>(R.id.labelTypical)
        nontypicalLabel = root.findViewById<TextView>(R.id.labelNontypical)
        typicalSwitch = root.findViewById<SwitchMaterial>(R.id.typicalSwitch)

        photoContainer = root.findViewById(R.id.photoContainer)
        buckImageView = root.findViewById(R.id.buckImageView)
        addPhotoText = root.findViewById(R.id.addPhotoText)

        downloadBtn = root.findViewById(R.id.downloadBtn)
        saveBtn = root.findViewById(R.id.saveBtn)


        // ============================
        // Setup Toolbar
        // ============================
        speciesToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_change_species -> {
                    showSpeciesSelector()
                    true
                }
                else -> false
            }
        }

        // ============================================
        // Listeners
        // ============================================
        photoContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

//        downloadBtn.setOnClickListener{
//            val rootView = root.findViewById<View>(android.R.id.content)
//            Snackbar.make(rootView, "Downloading PDF…", Snackbar.LENGTH_SHORT).show()
//            generateScorePdf()
//            Snackbar.make(rootView, "Downloaded successfully", Snackbar.LENGTH_SHORT).show()
//        }
        downloadBtn.setOnClickListener {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Generating PDF…", Snackbar.LENGTH_SHORT).show()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    generateScorePdf()
                }

                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Downloaded successfully", Snackbar.LENGTH_SHORT).show()
            }
        }

        saveBtn.setOnClickListener{

        }

        setSpecies(Species.WHITETAIL)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                buckImageView.setImageURI(uri)
                buckImageView.visibility = View.VISIBLE
                addPhotoText.visibility = View.GONE
                buckPic = buckImageView.drawable?.toBitmap()
            }
        }


    // ===============================
    // SPECIES SELECTION
    // ===============================
    private fun showSpeciesSelector() {
        val speciesNames = Species.values().map { it.displayName }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Species")
            .setItems(speciesNames) { _, which ->
                val selected = Species.entries[which]
                setSpecies(selected)
            }
            .show()
    }

    private fun setSpecies(species: Species) {
        measurementStore.clear()
        //measurements.clear()
        clearScoreDisplay()
        clearFields()
        hideAllSections()

        currentSpecies = species
        currentProfile = species.profile()
        animalName.setText("New ${species.displayName}")
        root.findViewById<MaterialToolbar>(R.id.speciesToolbar)
            .title = species.displayName
        speciesImage.setImageResource(species.getImageRes())

        buildUI(currentProfile)

    }


    // ===============================
    // SETUP UI
    // ===============================

    fun buildUI(profile: ScoringProfile) {
        hideAllSections()

        for(section: Section in profile.getVisibleSections()) section.show()

        if(profile.getVisibleSections().contains(Section.TYPE)){
            typicalSwitch.isChecked = true
            handleSwitch()
        }

        for (config in profile.getSectionConfigs()) {
            val sectionView = sectionViews[config.type] ?: continue

            //sectionView.root.visibility = View.VISIBLE

            // Set text
            sectionView.title.text = config.title
            sectionView.note.text = config.note

            sectionView.subNote?.let {
                if (config.subNote != null) {
                    it.visibility = View.VISIBLE
                    it.text = config.subNote
                } else {
                    it.visibility = View.GONE
                }
            }

            // Clear old rows
            sectionView.container.removeAllViews()

            // Add rows
            for (row in config.rows) {
                row.layoutType.addRow(
                    sectionView.container,
                    row.label,
                    row.type,
                )
            }

            //var currentCount = config.rows.size
            // Handle dynamic rows
            sectionView.addButton?.let { btn ->
                if (config.maxDynamicRows > 0) {
                    btn.visibility = View.VISIBLE

                    btn.setOnClickListener {
                        val baseType = config.dynamicBaseType ?: return@setOnClickListener
                        var currentCount = getNextIndex(baseType)//config.rows.size//I1
                        if (currentCount < config.maxDynamicRows) {
                            val newIndex = currentCount + 1

                            val newRow = RowConfig(
                                label = dynamicLabelFor(rowTypeForDynamic(config.dynamicBaseType, newIndex), newIndex),
                                type = rowTypeForDynamic(config.dynamicBaseType, newIndex),
                                layoutType = RowLayoutType.LEFT_RIGHT
                            )

                            newRow.layoutType.addRow(
                                sectionView.container,
                                newRow.label,
                                newRow.type
                            )

                            currentCount++
                            if(currentCount >= config.maxDynamicRows)
                                btn.visibility = View.GONE
                        }
                    }

                } else {
                    btn.visibility = View.GONE
                }
            }
        }
    }

    private fun initSectionViews(view: View) {
        sectionViews = mapOf(
            Section.SPREADS to createSectionView(view, R.id.spreadsSection),
            Section.LENGTHS to createSectionView(view, R.id.lengthsSection),
            Section.ABNORMALS to createSectionView(view, R.id.abnormalPointsSection),
            Section.CIRCUMFERENCES to createSectionView(view, R.id.circumferenceSection),
            Section.POINT_COUNT to createSectionView(view, R.id.pointsSection),
            Section.WIDTHS to createSectionView(view, R.id.widthsSection),
            Section.CROWN_POINTS to createSectionView(view, R.id.crownPointsSection)

            //Section.TYPE to createSectionView(view, R.id.typeSection)
        )
    }

    private fun createSectionView(root: View, sectionId: Int): SectionView {
        val section = root.findViewById<View>(sectionId)

        return SectionView(
            root = section,
            container = section.findViewById(R.id.rowsContainer),
            title = section.findViewById(R.id.sectionTitle),
            note = section.findViewById(R.id.sectionNote),
            subNote = section.findViewByIdOrNull(R.id.sectionSubnote),
            addButton = section.findViewByIdOrNull(R.id.addPointButton)
        )
    }

    fun <T : View> View.findViewByIdOrNull(id: Int): T? {
        return try {
            findViewById(id)
        } catch (e: Exception) {
            null
        }
    }

    private fun rowTypeForDynamic(type: MeasurementType, newIndex: Int): MeasurementType {
        return when (type) {
            is MeasurementType.G -> MeasurementType.G(newIndex)
            is MeasurementType.AbnormalPoint -> MeasurementType.AbnormalPoint(newIndex)
            is MeasurementType.Circumference -> MeasurementType.Circumference(newIndex)
            is MeasurementType.CrownPoint -> MeasurementType.CrownPoint(newIndex)
            else -> type // fallback for non-indexed types
        }
    }

    private fun dynamicLabelFor(type: MeasurementType, index: Int): String {
        return when (type) {
            is MeasurementType.G -> {
                val ordinal = ordinalWord(index)
                "G$index: $ordinal Point"
            }
            is MeasurementType.AbnormalPoint -> {
                "Abnormal Point $index"
            }
            is MeasurementType.CrownPoint -> {
                "Crown Point $index"
            }
            is MeasurementType.Circumference -> {
                "Circumference $index"
            }
            else -> "$index"
        }
    }

    fun Section.show() = when (this) {
        Section.TYPE -> showTypeSection()
        Section.SPREADS -> showSpreadsSection()
        Section.POINT_COUNT -> showPointsSection()
        Section.ABNORMALS -> showAbnormalsSection()
        Section.LENGTHS -> showLengthsSection()
        Section.WIDTHS -> showWidthsSection()
        Section.CROWN_POINTS -> showCrownPointsSection()
        Section.CIRCUMFERENCES -> showCircumferenceSection() }

    fun RowLayoutType.addRow(container: LinearLayout, label: String, type: MeasurementType) = when (this) {
        RowLayoutType.SINGLE -> addSingleMeasurementRow(container, label, type)
        RowLayoutType.LEFT_RIGHT_NO_FRAC -> addNoFracRow(container, label, type)
        RowLayoutType.LEFT_RIGHT -> addStandardRow(container, label, type)
        RowLayoutType.LOC -> addLocRow(container, label, type)
    }

    private fun addStandardRow(
        container: LinearLayout,
        label: String,
        type: MeasurementType
    ) {
        val row = layoutInflater.inflate(
            R.layout.row_left_right,
            container,
            false
        )

        if (label == "")
            row.findViewById<TextView>(R.id.measurementLabel).visibility = View.GONE
        else
            row.findViewById<TextView>(R.id.measurementLabel).text = label

        registerMeasurementViews(
            type,
            Side.LEFT,
            row.findViewById<EditText>(R.id.leftInches),
            row.findViewById<Spinner>(R.id.leftFraction)
        )

        registerMeasurementViews(
            type,
            Side.RIGHT,
            row.findViewById<EditText>(R.id.rightInches),
            row.findViewById<Spinner>(R.id.rightFraction)
        )

        container.addView(row)
    }

    private fun addSingleMeasurementRow(
        container: LinearLayout,
        label: String,
        type: MeasurementType
    ) {
        val row = layoutInflater.inflate(
            R.layout.row_single_measurement,
            container,
            false
        )

        if (label == "")
            row.findViewById<TextView>(R.id.measurementLabel).visibility = View.GONE
        else
            row.findViewById<TextView>(R.id.measurementLabel).text = label


        registerMeasurementViews(
            type,
            null,
            row.findViewById<EditText>(R.id.inches),
            row.findViewById<Spinner>(R.id.fraction)
        )

        container.addView(row)
    }

    private fun addNoFracRow(
        container: LinearLayout,
        label: String,
        type: MeasurementType
    ) {
        val row = layoutInflater.inflate(
            R.layout.row_left_right_no_frac,
            container,
            false
        )

        if (label == "")
            row.findViewById<TextView>(R.id.measurementLabel).visibility = View.GONE
        else
            row.findViewById<TextView>(R.id.measurementLabel).text = label


        registerMeasurementViews(
            type,
            Side.LEFT,
            row.findViewById<EditText>(R.id.left),
            null
        )

        registerMeasurementViews(
            type,
            Side.RIGHT,
            row.findViewById<EditText>(R.id.right),
            null
        )

        container.addView(row)
    }

    private fun addLocRow(
        container: LinearLayout,
        label: String,
        type: MeasurementType
    ) {
        val row = layoutInflater.inflate(
            R.layout.row_left,
            container,
            false
        )

        if (label == "")
            row.findViewById<TextView>(R.id.measurementLabel).visibility = View.GONE
        else
            row.findViewById<TextView>(R.id.measurementLabel).text = label


        registerMeasurementViews(
            type,
            null,//Side.LEFT,
            row.findViewById<EditText>(R.id.inches),
            row.findViewById<Spinner>(R.id.fraction)
        )

        container.addView(row)
    }

    private fun scoreCardSetup(config: ScoreDisplayConfig){
        if (config.showAbnormals)
            root.findViewById<LinearLayout>(R.id.abnormalScoreLabel).visibility = View.VISIBLE
        else
            root.findViewById<LinearLayout>(R.id.abnormalScoreLabel).visibility = View.GONE

        if (config.showSpread)
            root.findViewById<LinearLayout>(R.id.spreadScoreLabel).visibility = View.VISIBLE
        else
            root.findViewById<LinearLayout>(R.id.spreadScoreLabel).visibility = View.GONE

        if (config.showCrownPointScore)
            root.findViewById<LinearLayout>(R.id.crownScoreLabel).visibility = View.VISIBLE
        else
            root.findViewById<LinearLayout>(R.id.crownScoreLabel).visibility = View.GONE


    }


    // ===============================
    // SETUP ANIMALS
    // ===============================

    fun Species.profile(): ScoringProfile = when (this) {
        Species.WHITETAIL,
        Species.COUES,
        Species.SITKA_BLACKTAIL,
        Species.MULE_DEER,
        Species.COLUMBIA_BLACKTAIL -> DeerProfile()

        Species.ROCKY_MOUNTAIN_ELK -> RockyMountainElkProfile()
        Species.TULE_ELK,
        Species.ROOSEVELT_ELK -> WesternElkProfile()

        Species.SHIRAS_MOOSE,
        Species.CANADA_MOOSE,
        Species.YUKON_MOOSE -> MooseProfile()

        Species.BARREN_GROUND_CARIBOU,
        Species.CC_BARREN_GROUND_CARIBOU,
        Species.MOUNTAIN_CARIBOU,
        Species.QUEBEC_LABRADOR_CARIBOU,
        Species.WOODLAND_CARIBOU -> CaribouProfile()

        Species.BIGHORN_SHEEP,
        Species.DALL_SHEEP,
        Species.DESERT_SHEEP,
        Species.STONE_SHEEP -> SheepProfile(countBeamDiff = false)
        Species.MOUNTAIN_GOAT,
        Species.MUSK_OX,
        Species.BISON -> SheepProfile(countBeamDiff = true)

        Species.PRONGHORN -> PronghornProfile()
    }



    // =========================
    // Switch handling
    // =========================

    private fun handleSwitch() {

        fun updateModeLabels(isTypical: Boolean) {
            typicalLabel.alpha = if (isTypical) 1.0f else 0.4f
            nontypicalLabel.alpha = if (isTypical) 0.4f else 1.0f
        }

        typicalSwitch.setOnCheckedChangeListener { _, isChecked ->
            buckType = if (isChecked) {
                BuckType.TYPICAL
            } else {
                BuckType.NONTYPICAL
            }
            updateModeLabels(isChecked)
            recalcScore()
        }

        // make labels clickable
        typicalLabel.setOnClickListener {
            typicalSwitch.isChecked = true
        }
        nontypicalLabel.setOnClickListener {
            typicalSwitch.isChecked = false
        }

        // initialize state
        updateModeLabels(typicalSwitch.isChecked)
    }

    // ===============================
    // REGISTRATION / FIELD LISTENERS
    // ===============================

    private fun registerMeasurementViews(
        type: MeasurementType,
        side: Side?,
        inchesField: EditText,
        fractionField: Spinner? = null
    ) {
        val field = MeasurementField(type, side, inchesField, fractionField)

        fun updateStore() {
            measurementStore.update(field)
        }

        // Initial value
        updateStore()

        // Text changes
        inchesField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateStore()
                recalcScore()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Spinner changes (if exists)
        fractionField?.let {
            when(type){
                is MeasurementType.Loc -> loadLocFractions(it)
                else -> loadEighths(it)
            }
            it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateStore()
                    recalcScore()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }



    // ===============================
    // SCORING LOGIC
    // ===============================

    private fun recalcScore() {
        currentScore = currentProfile?.calculateScore(measurementStore, buckType)
            ?: return
        val display = currentProfile?.getScoreDisplayConfig() ?: return
        scoreCardSetup(display)


        if (currentScore.spreadIsCapped){
            spreadCreditLabel.visibility = View.VISIBLE
            spreadLabel.visibility = View.GONE
            spreadCreditNote.visibility = View.VISIBLE
        }
        else{
            spreadCreditLabel.visibility = View.GONE
            spreadLabel.visibility = View.VISIBLE
            spreadCreditNote.visibility = View.GONE
        }

        spreadCreditText.text = doubleToBC(currentScore.spreadCredit)
        leftSumText.text = doubleToBC(currentScore.leftSum)
        rightSumText.text = doubleToBC(currentScore.rightSum)
        subtotalText.text = doubleToBC(currentScore.subtotal)
        crownScoreText.text = doubleToBC(currentScore.crownPointScore)
        differenceText.text = doubleToBC(currentScore.differenceTotal)
        abnormalText.text = doubleToBC(currentScore.abnormalSum)
        grossScoreText.text = doubleToBC(currentScore.gross)

        finalScoreText.text = doubleToBC(currentScore.finalScore)

    }


    // ===============================
    // PDF Download Functionality
    // ===============================
    private fun generateScorePdf() {

        class PdfWriter(
            val pdf: PdfDocument,
            val paint: Paint
        ) {
            var pageNumber = 1
            var y = MARGIN

            private var pageInfo = PdfDocument.PageInfo
                .Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
                .create()

            private var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            private fun newPage() {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo
                    .Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
                    .create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            fun ensureSpace(height: Int) {
                if (y + height > PAGE_HEIGHT - MARGIN) {
                    newPage()
                }
            }

            fun text(text: String, height: Int = LINE_HEIGHT) {
                ensureSpace(height)
                canvas.drawText(text, 40f, y.toFloat(), paint)
                y += height
            }

            fun header(text: String, height: Int = LINE_HEIGHT){
                ensureSpace(height)
                paint.textSize = 18f
                paint.isFakeBoldText = true
                text(text, height)
            }

            fun row(label: String, left: String, right: String, diff: String, loc: String = "") {
                ensureSpace(LINE_HEIGHT)
                canvas.drawText(label, 40f, y.toFloat(), paint)
                canvas.drawText(left, 220f, y.toFloat(), paint)
                canvas.drawText(right, 300f, y.toFloat(), paint)
                canvas.drawText(diff, 380f, y.toFloat(), paint)
                canvas.drawText(loc, 460f, y.toFloat(), paint)
                y += LINE_HEIGHT
            }

            fun line() {
                ensureSpace(LINE_HEIGHT)
                canvas.drawLine(40f, y.toFloat(), 500f, y.toFloat(), paint)
                y += LINE_HEIGHT
            }

            fun spacing(spacing: Int)
            {
                ensureSpace(spacing)
                y += spacing
            }

            fun image(bitmap: Bitmap, maxWidth: Int = 240, maxHeight: Int = 300) {
                val scaled = scaleBitmap(bitmap, maxWidth, maxHeight)

                ensureSpace(scaled.height + LINE_HEIGHT)
                val start = (PAGE_WIDTH - scaled.width)/2
                val rect = Rect(start, y, start + scaled.width, y + scaled.height)
                canvas.drawBitmap(scaled, null, rect, null)
                y += scaled.height + LINE_HEIGHT
            }

            fun finish() {
                pdf.finishPage(page)
            }
        }

        val pdf = PdfDocument()
        val paint = Paint()
        val writer = PdfWriter(pdf, paint)
        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val all = measurementStore.getAll()
        val sections = currentProfile.getVisibleSections()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        writer.text("${currentSpecies.displayName} Score Sheet", 28)

        paint.textSize = 14f
        paint.isFakeBoldText = false

        writer.text("Name: ${animalName.text}", 28)

        // TYPE
        if (sections.contains(Section.TYPE))
            writer.text("Type: ${buckType.name}", 28)

        writer.text("Date: ${date.format(formatter)}")
        writer.spacing(10)

        // Point Count
        if(sections.contains(Section.POINT_COUNT))
        {
            writer.header("Number of Points", 10)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            writer.line()

            val section = currentProfile.getSectionConfigs().find{it.type == Section.POINT_COUNT}
            val rows  = section?.rows

            if (rows != null) {
                paint.isFakeBoldText = true
                if (rows.find { it.showDifference == true } != null)
                    writer.row("Measurement", "Left", "Right", "Difference")
                else
                    writer.row("Measurement", "Left", "Right", "")
                paint.isFakeBoldText = false

                for (row in rows) {
                    var diff = ""
                    if (row.showDifference)
                        diff = doubleToBC(measurementStore.getPaired(row.type).difference())

                    var label = row.label
                    if(row.type == MeasurementType.PointCount && row.label == "")
                        label = "Normal Points"

                    writer.row(
                        label, doubleToBC(measurementStore.get(row.type, Side.LEFT)),
                        doubleToBC(measurementStore.get(row.type, Side.RIGHT)), diff)

                }
            }
            writer.spacing(10)
        }

        // SPREADS
        // Potential issues:
        // Doesn't account for spread display order
        if(sections.contains(Section.SPREADS))
        {
            writer.header("Spreads", 10)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            writer.line()

            val section = currentProfile.getSectionConfigs().find{it.type == Section.SPREADS}
            val rows  = section?.rows

            if (rows != null) {
                for(row in rows){
                    writer.text(row.label + ":\t ${doubleToBC(measurementStore.get(row.type))}")
                }
            }
            writer.spacing(10)
        }


        // LENGTHS
        // Potential issues:
        // Labels for G points might be different (i.e. caribou) - needs RowConfigs
        // Doesn't account for sheep not having difference in main beams
        if(sections.contains(Section.LENGTHS))
        {
            writer.header("Lengths", 10)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            writer.line()

            val section = currentProfile.getSectionConfigs().find{it.type == Section.LENGTHS}
            val rows  = section?.rows

            paint.isFakeBoldText = true

            val mainBeams = measurementStore.getPaired(MeasurementType.MainBeam)

            if (rows != null) {
                paint.isFakeBoldText = true
                if (rows.find { it.showDifference == true } != null)
                    writer.row("Measurement", "Left", "Right", "Difference")
                else
                    writer.row("Measurement", "Left", "Right", "")
                paint.isFakeBoldText = false

                for (row in rows) {

                    var diff = ""
                    if (row.showDifference)
                        diff = doubleToBC(measurementStore.getPaired(row.type).difference())

                    var label = row.label
                    if(row.type == MeasurementType.MainBeam && row.label == "")
                        label = "Length of horns"

                    writer.row(
                        label, doubleToBC(measurementStore.get(row.type, Side.LEFT)),
                        doubleToBC(measurementStore.get(row.type, Side.RIGHT)), diff)

                }
            }

            // For dynamically added points
            val gPoints = measurementStore.getPairedList{it is MeasurementType.G}.filter{it.left + it.right > 0.0}
            for (p in gPoints) {
                if (rows?.find{it.type == p.type} != null){
                    continue
                }
                var displayName = rows?.find{it.type == p.type}?.label
                if(displayName == null || displayName == "")
                    displayName = p.type.displayMeasurementName()
                writer.row(
                    displayName,
                    doubleToBC(p.left),
                    doubleToBC(p.right),
                    doubleToBC(p.difference())
                )
            }

            writer.spacing(10)
        }

        //WIDTHS
        if(sections.contains(Section.WIDTHS))
        {
            writer.header("Widths", 10)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            writer.line()

            paint.isFakeBoldText = true
            writer.row("Measurement","Left","Right","Difference")
            paint.isFakeBoldText = false

            val section = currentProfile.getSectionConfigs().find{it.type == Section.WIDTHS}
            val rows  = section?.rows


            if (rows != null) {
                for(row in rows){
                    var diff = ""
                    if(row.showDifference)
                        diff = doubleToBC(measurementStore.getPaired(row.type).difference())
                    writer.row(row.label, doubleToBC(measurementStore.get(row.type, Side.LEFT)),
                        doubleToBC(measurementStore.get(row.type, Side.RIGHT)), diff)
                }
            }
            writer.spacing(10)
        }

        //CIRCUMFERENCES
        if(sections.contains(Section.CIRCUMFERENCES))
        {
            writer.header("Circumferences", 10)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            writer.line()

            var locLabel = ""
            if(all.find{it.type is MeasurementType.Loc} != null){
                locLabel = "Location"
            }

            paint.isFakeBoldText = true
            writer.row("Measurement","Left","Right","Difference", locLabel)
            paint.isFakeBoldText = false

            val section = currentProfile.getSectionConfigs().find{it.type == Section.CIRCUMFERENCES}
            val rows  = section?.rows

            val circumferences = measurementStore.getPairedList { it is MeasurementType.Circumference }

            for (c in circumferences) {
                val type = c.type as MeasurementType.Circumference
                val index = type.index

                val locType = MeasurementType.Loc(index)
                var locValue = ""
                if (all.any{it.type == locType}) {
                   locValue = doubleToBC(measurementStore.get(locType))
                }


                writer.row(
                    c.type.displayMeasurementName(),
                    doubleToBC(c.left),
                    doubleToBC(c.right),
                    doubleToBC(c.difference()),
                    locValue
                )

            }

            writer.spacing(10)
        }

        writer.row("Totals", doubleToBC(currentScore.leftSum), doubleToBC(currentScore.rightSum), doubleToBC(currentScore.differenceTotal))

        if(sections.contains(Section.CROWN_POINTS)){
            val crownPoints = measurementStore.getAll().filter { it.type is MeasurementType.CrownPoint }
            writer.row("Crown Totals", doubleToBC(crownPoints.filter{ it.side == Side.LEFT }.sumOf{it.value}), doubleToBC(crownPoints.filter{ it.side == Side.RIGHT }.sumOf{it.value}), "")

        }

        if(sections.contains(Section.ABNORMALS)){
            val abnormalPoints = measurementStore.getAll().filter { it.type is MeasurementType.AbnormalPoint }
            writer.row("Abnormal Totals", doubleToBC(abnormalPoints.filter{ it.side == Side.LEFT }.sumOf{it.value}), doubleToBC(abnormalPoints.filter{ it.side == Side.RIGHT }.sumOf{it.value}), "")

        }

        writer.line()
        paint.isFakeBoldText = true
        writer.row("Gross Score", "", "", doubleToBC(currentScore.gross))
        writer.row("Net Score", "", "", doubleToBC(currentScore.finalScore))

        buckPic?.let { writer.image(it) }

        writer.finish()
        savePdfToDownloads(pdf)
    }


    private fun savePdfToDownloads(pdf: PdfDocument) {
        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss")
        val dateStr = date.format(formatter)
        val safeName = animalName.text.toString().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val filename = "${safeName}_${dateStr}"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = requireContext().contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            values
        )

        uri?.let {
            requireContext().contentResolver.openOutputStream(it).use { output ->
                pdf.writeTo(output)
            }
        }
        pdf.close()
    }

    fun MeasurementType.displayMeasurementName(): String {
        return when (this) {
            is MeasurementType.G -> "G${index}: ${ordinalWord(index)} Point"
            is MeasurementType.AbnormalPoint -> "Abnormal ${index}"
            MeasurementType.MainBeam -> "Main Beam"
            is MeasurementType.Circumference -> "Circumference ${index}"
            MeasurementType.InnerSpread -> "Inner Spread"
            MeasurementType.GreatestSpread -> "Greatest Spread"
            MeasurementType.TipSpread -> "Tip to Tip Spread"
            MeasurementType.PointCount -> "Number of Points"
            MeasurementType.Width -> "Width of Palms"
            MeasurementType.BrowWidth -> "Width of Brow Palms"
            MeasurementType.ProngLength -> "Prong Length"
            MeasurementType.BrowPointCount -> "Number of Brow Points"
            is MeasurementType.CrownPoint -> "Crown Point ${index}"
            is MeasurementType.Loc -> "Location of Measurement ${index}"
        }
    }

    // ===============================
    // CLEARING / RESTORING
    // ===============================
    private fun clearScoreDisplay() {
        grossScoreText.text = "0"
        crownScoreText.text = "0"
        abnormalText.text = "0"
        differenceText.text = "0"
        spreadCreditText.text = "0"
        leftSumText.text = "0"
        rightSumText.text = "0"
        subtotalText.text = "0"
        finalScoreText.text = "0"
    }

    private fun clearFields() {
        clearSection(R.id.pointsSection)
        clearSection(R.id.abnormalPointsSection)
        clearSection(R.id.lengthsSection)
        clearSection(R.id.spreadsSection)
        clearSection(R.id.circumferenceSection)
        clearSection(R.id.crownPointsSection)
        clearSection(R.id.widthsSection)
        buckPic = null
        buckImageView.visibility = View.GONE
        root.findViewById<TextView>(R.id.addPhotoText).visibility = View.VISIBLE

    }

    private fun clearSection(sectionId: Int) {
        val section = root.findViewById<View>(sectionId) ?: return
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer) ?: return
        container.removeAllViews()
    }

    // ===============================
    // HIDE / SHOW SECTION METHODS
    // ===============================

    fun hideAllSections() {
        root.findViewById<LinearLayout>(R.id.abnormalPointsSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.lengthsSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.spreadsSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.circumferenceSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.pointsSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.typeSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.widthsSection).visibility = View.GONE
        root.findViewById<LinearLayout>(R.id.crownPointsSection).visibility = View.GONE
    }

    fun showTypeSection(){
        root.findViewById<LinearLayout>(R.id.typeSection).visibility = View.VISIBLE
    }

    fun showPointsSection(){
        root.findViewById<LinearLayout>(R.id.pointsSection).visibility = View.VISIBLE
    }

    fun showSpreadsSection(){
        root.findViewById<LinearLayout>(R.id.spreadsSection).visibility = View.VISIBLE
    }

    fun showAbnormalsSection(){
        root.findViewById<LinearLayout>(R.id.abnormalPointsSection).visibility = View.VISIBLE
    }

    fun showCrownPointsSection(){
        root.findViewById<LinearLayout>(R.id.crownPointsSection).visibility = View.VISIBLE
    }

    fun showLengthsSection(){
        root.findViewById<LinearLayout>(R.id.lengthsSection).visibility = View.VISIBLE
    }

    fun showCircumferenceSection(){
        root.findViewById<LinearLayout>(R.id.circumferenceSection).visibility = View.VISIBLE
    }

    fun showWidthsSection(){
        root.findViewById<LinearLayout>(R.id.widthsSection).visibility = View.VISIBLE
    }


    // ===============================
    // MISCELLANEOUS HELPERS
    // ===============================

    private fun loadEighths(spinner: Spinner) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_fraction,
            eighthFractions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_fraction)
        spinner.adapter = adapter
    }

    private fun loadLocFractions(spinner: Spinner) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_fraction,
            locationFractions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_fraction)
        spinner.adapter = adapter
    }


    private fun ordinalWord(index: Int): String {
        return when (index) {
            1 -> "First"
            2 -> "Second"
            3 -> "Third"
            4 -> "Fourth"
            5 -> "Fifth"
            6 -> "Sixth"
            7 -> "Seventh"
            8 -> "Eighth"
            9 -> "Ninth"
            10 -> "Tenth"
            11 -> "Eleventh"
            12 -> "Twelfth"
            13 -> "Thirteenth"
            14 -> "Fourteenth"
            15 -> "Fifteenth"
            else -> index.toString()
        }
    }


    private fun doubleToBC(value: Double): String {
        val whole = value.toInt()
        val frac = ((value - whole) * 8).roundToInt()

        return when (frac) {
            0 -> "$whole"
            8 -> "${whole + 1}"
            else -> "$whole $frac/8"
        }
    }

    fun scaleBitmap(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun getNextIndex(
        baseType: MeasurementType
    ): Int {
        return when (baseType) {
            is MeasurementType.G -> {
                measurementStore.getAll()
                    .mapNotNull { it.type as? MeasurementType.G }
                    .maxOfOrNull { it.index } ?: 1
            }

            is MeasurementType.AbnormalPoint -> {
                measurementStore.getAll()
                    .mapNotNull { it.type as? MeasurementType.AbnormalPoint }
                    .maxOfOrNull { it.index } ?: 1
            }

            is MeasurementType.Circumference -> {
                measurementStore.getAll()
                    .mapNotNull { it.type as? MeasurementType.Circumference }
                    .maxOfOrNull { it.index }?: 1
            }

            is MeasurementType.CrownPoint -> {
                measurementStore.getAll()
                    .mapNotNull { it.type as? MeasurementType.CrownPoint }
                    .maxOfOrNull { it.index }?: 1
            }

            else -> 1
        }
    }

    fun Species.getImageRes(): Int {
        return when (this) {
            Species.WHITETAIL,
            Species.COUES -> R.drawable.final_white_deer_nobg

            Species.COLUMBIA_BLACKTAIL,
            Species.SITKA_BLACKTAIL,
            Species.MULE_DEER -> R.drawable.final_white_mule_deer_nobg

            Species.ROCKY_MOUNTAIN_ELK -> R.drawable.final_white_rocky_mountain_elk_nobg
            Species.ROOSEVELT_ELK,
            Species.TULE_ELK -> R.drawable.final_white_western_elk_nobg

            Species.SHIRAS_MOOSE,
            Species.CANADA_MOOSE,
            Species.YUKON_MOOSE -> R.drawable.final_white_moose_nobg

            Species.CC_BARREN_GROUND_CARIBOU,
            Species.WOODLAND_CARIBOU,
            Species.MOUNTAIN_CARIBOU,
            Species.BARREN_GROUND_CARIBOU,
            Species.QUEBEC_LABRADOR_CARIBOU -> R.drawable.final_white_caribou_nobg

            Species.BIGHORN_SHEEP,
            Species.STONE_SHEEP,
            Species.DALL_SHEEP,
            Species.DESERT_SHEEP -> R.drawable.final_white_sheep_nobg

            Species.PRONGHORN -> R.drawable.final_white_sheep_nobg
            Species.MUSK_OX -> R.drawable.final_white_musk_ox_nobg
            Species.BISON -> R.drawable.final_white_sheep_nobg
            Species.MOUNTAIN_GOAT -> R.drawable.final_white_mountain_goat_nobg
        }
    }


}