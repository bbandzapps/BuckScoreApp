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
import android.graphics.Rect
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.text.font.Typeface
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import java.io.OutputStream

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
        ELK("Elk"),
        MOOSE("Moose"),
        BIGHORN_SHEEP("Bighorn Sheep"),
        DALLS_SHEEP("Dall's Sheep"),
        DESERT_SHEEP("Desert Sheep"),
        STONES_SHEEP("Stone's Sheep"),
        MOUNTAIN_GOAT("Mountain Goat"),
        //        CARIBOU,
        BISON("Bison"),
        MUSK_OX("Musk Ox")
//        PRONGHORN
    }

    enum class Section{
        TYPE,
        POINT_COUNT,
        SPREADS,
        ABNORMALS,
        LENGTHS,
        WIDTHS,
        //CROWN_POINTS,
        CIRCUMFERENCES
    }

    enum class RowLayoutType {
        LEFT_RIGHT,          // addStandardRow
        SINGLE,              // addSingleMeasurementRow
        LEFT_RIGHT_NO_FRAC   // addNoFracRow
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
                fraction = fractionField.selectedItemPosition / 8.0

            return whole + fraction
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
        object Width: MeasurementType()
    }

    enum class Side { LEFT, RIGHT }

    data class ScoreBreakdown(
        val mainBeams: PairedMeasurement,
        val gPoints: List<PairedMeasurement>,
        val abnormalPoints: List<MeasurementValue>,
        val circumferences: List<PairedMeasurement>,
        val innerSpread: Double,
        val leftSum: Double,
        val rightSum: Double,
        val differenceTotal: Double,
        val abnormalSum: Double,
        val spreadCredit: Double,
        val subtotal: Double,
        val gross: Double,
        val finalScore: Double
    )

    data class MeasurementValue(
        val type: MeasurementType,
        val side: Side?,
        val value: Double
    )

    //I1
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
        val layoutType: RowLayoutType
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


    private lateinit var spreadCreditLabel: TextView
    private lateinit var spreadLabel: TextView
    private lateinit var spreadCreditNote: TextView

    private lateinit var grossScoreText: TextView
    private lateinit var differenceText: TextView
    private lateinit var abnormalText: TextView
    private lateinit var spreadCreditText: TextView
    private lateinit var leftSumText: TextView
    private lateinit var rightSumText: TextView
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root = view
        initSectionViews(view)

        val speciesToolbar = root.findViewById<MaterialToolbar>(R.id.speciesToolbar)

        // ============================
        // Bind ALWAYS-EXISTING VIEWS
        // ============================
        animalName = root.findViewById(R.id.animal_name)


        grossScoreText = root.findViewById(R.id.grossScoreText)
        abnormalText = root.findViewById(R.id.abnormalText)
        differenceText = root.findViewById(R.id.differenceText)
        spreadCreditText = root.findViewById(R.id.spreadCreditText)
        leftSumText = root.findViewById(R.id.leftSumText)
        rightSumText = root.findViewById(R.id.rightSumText)
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

        downloadBtn.setOnClickListener{
            val rootView = root.findViewById<View>(android.R.id.content)
            Snackbar.make(rootView, "Downloading PDF…", Snackbar.LENGTH_SHORT).show()
            generateScorePdf()
            Snackbar.make(rootView, "Downloaded successfully", Snackbar.LENGTH_SHORT).show()
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

        buildUI(currentProfile)


        // Update toolbar title

        // Reset scoring layout based on species
//        when (currentSpecies) {
//            Species.WHITETAIL,
//            Species.SITKA_BLACKTAIL,
//            Species.MULE_DEER,
//            Species.COUES,
//            Species.COLUMBIA_BLACKTAIL -> setupDeer(currentSpecies)
//
//            Species.ELK -> setupElk(currentSpecies)
//            Species.MOOSE -> setupMoose(currentSpecies)
//
//            Species.BISON,
//            Species.MOUNTAIN_GOAT,
//            Species.MUSK_OX,
//            Species.DALLS_SHEEP,
//            Species.DESERT_SHEEP,
//            Species.STONES_SHEEP,
//            Species.BIGHORN_SHEEP -> setupSheep(currentSpecies)
//        }
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

            // Handle dynamic rows
            sectionView.addButton?.let { btn ->
                if (config.maxDynamicRows > 0) {
                    btn.visibility = View.VISIBLE

                    var currentCount = config.rows.size

                    btn.setOnClickListener {
                        val baseType = config.dynamicBaseType ?: return@setOnClickListener
                        if (currentCount < config.maxDynamicRows) {
                            val newIndex = currentCount + 1

                            val newRow = RowConfig(
                                label = "${config.type.name} $newIndex",
                                type = rowTypeForDynamic(config.dynamicBaseType, newIndex),
                                layoutType = RowLayoutType.LEFT_RIGHT
                            )

                            newRow.layoutType.addRow(
                                sectionView.container,
                                newRow.label,
                                newRow.type
                            )

                            currentCount++
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
            Section.WIDTHS to createSectionView(view, R.id.widthsSection)

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
            else -> type // fallback for non-indexed types
        }
    }

    fun Section.show() = when (this) {
        Section.TYPE -> showTypeSection()
        Section.SPREADS -> showSpreadsSection()
        Section.POINT_COUNT -> showPointsSection()
        Section.ABNORMALS -> showAbnormalsSection()
        Section.LENGTHS -> showLengthsSection()
        Section.WIDTHS -> showWidthsSection()
        Section.CIRCUMFERENCES -> showCircumferenceSection() }

    fun RowLayoutType.addRow(container: LinearLayout, label: String, type: MeasurementType) = when (this) {
        RowLayoutType.SINGLE -> addSingleMeasurementRow(container, label, type)
        RowLayoutType.LEFT_RIGHT_NO_FRAC -> addNoFracRow(container, label, type)
        RowLayoutType.LEFT_RIGHT -> addStandardRow(container, label, type)
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


    // ===============================
    // SETUP ANIMALS
    // ===============================

    fun Species.profile(): ScoringProfile = when (this) {
        Species.WHITETAIL,
        Species.COUES,
        Species.SITKA_BLACKTAIL,
        Species.MULE_DEER,
        Species.COLUMBIA_BLACKTAIL -> DeerProfile()

        Species.ELK -> DeerProfile()
        Species.MOOSE -> MooseProfile()

        Species.BIGHORN_SHEEP,
        Species.DALLS_SHEEP,
        Species.DESERT_SHEEP,
        Species.STONES_SHEEP,
        Species.MOUNTAIN_GOAT,
        Species.MUSK_OX,
        Species.BISON -> SheepProfile()
        //Species.PRONGHORN -> PronghornProfile()
        //Species.CARIBOU -> CaribouProfile()
    }

    fun setupDeer(species: Species){

        root.findViewById<LinearLayout>(R.id.abnormalPointsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.pointsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.typeSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.spreadsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.lengthsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.circumferenceSection).visibility = View.VISIBLE


        handleSwitch()
        //setupDeerPoints()
        //setupDeerSpreads()
        //setupDeerCircumferences()
        //setupDeerAbnormals()
        //setupDeerLengths()
    }

    fun setupDeerLengths() {

        val section = root.findViewById<View>(R.id.lengthsSection)
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer)
        val addBtn = section.findViewById<Button>(R.id.addPointButton)
        val title = section.findViewById<TextView>(R.id.sectionTitle)
        val note = section.findViewById<TextView>(R.id.sectionNote)

        container.removeAllViews()

        title.text = "Lengths"
        note.text = "Length of regular points and main beams"

        // Main beams first
        addStandardRow(
            container,
            "Main Beams",
            MeasurementType.MainBeam
        )

        // Default G1–G4
        for (i in 1..4) {
            addStandardRow(
                container,
                "G$i: ${ordinalWord(i)} Point",
                MeasurementType.G(i)
            )
        }

        var count = 4
        val max = 15

        addBtn.setOnClickListener {
            if (count >= max) { return@setOnClickListener }
            count++
            if (count >= max) { addBtn.visibility = View.GONE }
            addStandardRow(
                container,
                "G$count: ${ordinalWord(count)} Point",
                MeasurementType.G(count)
            )
        }
    }

    fun setupDeerAbnormals() {

        val section = root.findViewById<View>(R.id.abnormalPointsSection)
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer)
        val addBtn = section.findViewById<Button>(R.id.addPointButton)
        val title = section.findViewById<TextView>(R.id.sectionTitle)
        val note = section.findViewById<TextView>(R.id.sectionNote)

        container.removeAllViews()

        title.text = "Abnormal Points"
        note.text = "Length of abnormal points (droptines, points off of a burr, points off of other points, etc.)"

        // Default 2 rows
        for (i in 1..2) {
            addStandardRow(
                container,
                "Abnormal Point $i",
                MeasurementType.AbnormalPoint(i)
            )
        }

        var count = 2
        val max = 50

        addBtn.setOnClickListener {
            if (count >= max) { return@setOnClickListener }
            count++
            if (count >= max) { addBtn.visibility = View.GONE }
            addStandardRow(
                container,
                "Abnormal Point $count",
                MeasurementType.AbnormalPoint(count),
            )
        }
    }

    fun setupDeerCircumferences() {

        val section = root.findViewById<View>(R.id.circumferenceSection)
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer)
        val title = section.findViewById<TextView>(R.id.sectionTitle)
        val note = section.findViewById<TextView>(R.id.sectionNote)
        val subnote = section.findViewById<TextView>(R.id.sectionSubnote)

        container.removeAllViews()

        title.text = "Circumferences"
        note.text = "Smallest circumferences of main beam between points"
        subnote.text = "Note: If a buck does not have enough points to record all circumference measurements, the circumference between the main beam and the last point should be done halfway between the tip of the beam and the base of the last point."

        addStandardRow(container, "Smallest circumference between G1 and the burr", MeasurementType.Circumference(1))
        for (i in 2..4) {
            var j = i-1
            addStandardRow(
                container,
                "Smallest circumference between G$j and G$i",
                MeasurementType.Circumference(i)
            )
        }
    }

    fun setupDeerSpreads() {

        val section = root.findViewById<View>(R.id.spreadsSection)
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer)
        val title = section.findViewById<TextView>(R.id.sectionTitle)
        val note = section.findViewById<TextView>(R.id.sectionNote)

        container.removeAllViews()

        title.text = "Spreads"
        note.text = "Distance that the antlers span"

        addSingleMeasurementRow(container, "Tip to Tip (Main Beams)", MeasurementType.TipSpread)
        addSingleMeasurementRow(container, "Greatest Spread", MeasurementType.GreatestSpread)
        addSingleMeasurementRow(container, "Inner Spread", MeasurementType.InnerSpread)
    }

    fun setupDeerPoints() {

        val section = root.findViewById<View>(R.id.pointsSection)
        val container = section.findViewById<LinearLayout>(R.id.rowsContainer)
        val title = section.findViewById<TextView>(R.id.sectionTitle)
        val note = section.findViewById<TextView>(R.id.sectionNote)

        container.removeAllViews()

        title.text = "Points"
        note.text = "Number of points that are at least 1 inch long"

        addNoFracRow(container, "", MeasurementType.PointCount)
    }


    private fun setupElk(species: Species){
        handleSwitch()
        //setupDeerAbnormals()
        //setupDeerLengths()
    }

    private fun setupMoose(species: Species){
        handleSwitch()
        //setupDeerAbnormals()
        //setupDeerLengths()
    }

    private fun setupSheep(species: Species){
        root.findViewById<LinearLayout>(R.id.spreadsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.lengthsSection).visibility = View.VISIBLE
        root.findViewById<LinearLayout>(R.id.circumferenceSection).visibility = View.VISIBLE

        //setupDeerLengths()
        //setupDeerCircumferences()
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


//    private fun registerMeasurementViews(
//        type: MeasurementType,
//        side: Side?,
//        inchesField: EditText,
//        fractionField: Spinner? = null
//    ) {
//        if (fractionField == null){
//            inchesField.addTextChangedListener(sharedTextWatcher)
////            measurements.add(
////                MeasurementField(
////                    type,
////                    side,
////                    inchesField,
////                    null
////                )
////            )
//            measurementStore.update(
//                MeasurementField(
//                    type,
//                    side,
//                    inchesField,
//                    null
//                )
//            )
//        }
//        else{
//            loadEighths(fractionField)
//            inchesField.addTextChangedListener(sharedTextWatcher)
//            fractionField.onItemSelectedListener = sharedSpinnerListener
//
////            measurements.add(
////                MeasurementField(
////                    type,
////                    side,
////                    inchesField,
////                    fractionField
////                )
////            )
//
//            measurementStore.update(
//                MeasurementField(
//                    type,
//                    side,
//                    inchesField,
//                    fractionField
//                )
//            )
//        }
//    }
//
//
//    private val sharedTextWatcher = object : TextWatcher {
//        override fun afterTextChanged(s: Editable?) {
//            recalcScore()
//        }
//        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//    }
//
//    private val sharedSpinnerListener = object : AdapterView.OnItemSelectedListener {
//        override fun onItemSelected(
//            parent: AdapterView<*>?,
//            view: View?,
//            position: Int,
//            id: Long
//        ) {
//            recalcScore()
//        }
//
//        override fun onNothingSelected(parent: AdapterView<*>?) {}
//    }

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
            loadEighths(it)
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

        // ************NEED TO MOVE**************
        var spreadCredit = currentScore.innerSpread
        if (spreadCredit > max(currentScore.mainBeams.left, currentScore.mainBeams.right)){
            spreadCredit = max(currentScore.mainBeams.left, currentScore.mainBeams.right)
            spreadCreditLabel.visibility = View.VISIBLE
            spreadLabel.visibility = View.GONE
            spreadCreditNote.visibility = View.VISIBLE
        }
        else{
            spreadCreditLabel.visibility = View.GONE
            spreadLabel.visibility = View.VISIBLE
            spreadCreditNote.visibility = View.GONE
        }
        // ************************************

        spreadCreditText.text = "${doubleToBC(spreadCredit)}"
        leftSumText.text = "${doubleToBC(currentScore.leftSum)}"
        rightSumText.text = "${doubleToBC(currentScore.rightSum)}"
        subtotalText.text = "${doubleToBC(currentScore.subtotal)}"
        differenceText.text = "${doubleToBC(currentScore.differenceTotal)}"
        abnormalText.text = "${doubleToBC(currentScore.abnormalSum)}"
        grossScoreText.text = "${doubleToBC(currentScore.gross)}"

        finalScoreText.text = "${doubleToBC(currentScore.finalScore)}"

    }


    private fun scoreFromInput(
        inchesField: EditText,
        fractionSpinner: Spinner? = null
    ): Double {
        val whole = inchesField.text.toString().toIntOrNull() ?: 0
        if (fractionSpinner == null)
            return whole + 0.0

        val fractionIndex = fractionSpinner.selectedItemPosition
        return whole + (fractionIndex * 0.125)
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

            fun row(label: String, left: String, right: String, diff: String) {
                ensureSpace(LINE_HEIGHT)
                canvas.drawText(label, 40f, y.toFloat(), paint)
                canvas.drawText(left, 200f, y.toFloat(), paint)
                canvas.drawText(right, 300f, y.toFloat(), paint)
                canvas.drawText(diff, 420f, y.toFloat(), paint)
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
                canvas.drawBitmap(bitmap, null, rect, null)
                y += scaled.height + LINE_HEIGHT
            }

            fun finish() {
                pdf.finishPage(page)
            }
        }

        val pdf = PdfDocument()
        val paint = Paint()
        val writer = PdfWriter(pdf, paint)

        val score = currentScore

        paint.textSize = 24f
        paint.isFakeBoldText = true
        writer.text("Whitetail Buck Score Sheet", 28)

        paint.textSize = 14f
        paint.isFakeBoldText = false

        writer.text("Name: ${animalName.text}", 28)
        writer.text("Type: ${buckType.name}", 28)

        paint.textSize = 18f
        paint.isFakeBoldText = true
        writer.text("Spreads", 10)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        writer.line()

        val tipSpreadInches = tipSpreadInches.text.toString().toIntOrNull() ?: 0
        val tipSpreadFractionValue = tipSpreadFractions.selectedItemPosition / 8.0
        writer.text("Tip to Tip Spread: ${doubleToBC(tipSpreadInches + tipSpreadFractionValue)}",28)

        val greatestSpreadInches = greatestSpreadInches.text.toString().toIntOrNull() ?: 0
        val greatestSpreadFractionValue = greatestSpreadFractions.selectedItemPosition / 8.0
        writer.text("Greatest Spread: ${doubleToBC(greatestSpreadInches + greatestSpreadFractionValue)}",28)

        writer.text("Inner Spread: ${doubleToBC(score.innerSpread)}",28)

        paint.textSize = 18f
        paint.isFakeBoldText = true
        writer.text("Lengths", 10)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        writer.line()

        paint.isFakeBoldText = true
        writer.row("Measurement","Left","Right","Difference")
        paint.isFakeBoldText = false


        val beams = measurementStore.getPaired(MeasurementType.MainBeam)//score.mainBeams
        writer.row(beams.type.displayMeasurementName(), doubleToBC(beams.left), doubleToBC(beams.right), doubleToBC(beams.difference()))

        val validPoints = score.gPoints.filter{it.sum() > 0}
        for (p in validPoints) {
            writer.row(
                p.type.displayMeasurementName(),
                doubleToBC(p.left),
                doubleToBC(p.right),
                doubleToBC(p.difference())
            )
        }
        writer.spacing(10)

        paint.textSize = 18f
        paint.isFakeBoldText = true
        writer.text("Circumferences",10)
        paint.textSize = 14f
        paint.isFakeBoldText = false
        writer.line()

        paint.isFakeBoldText = true
        writer.row("Measurement","Left","Right","Difference")
        paint.isFakeBoldText = false

        for (p in score.circumferences) {
            writer.row(
                p.type.displayMeasurementName(),
                doubleToBC(p.left),
                doubleToBC(p.right),
                doubleToBC(p.difference())
            )
        }

        writer.line()

        writer.row("Totals", "${leftSumText.text}", "${rightSumText.text}", "${differenceText.text}")
        writer.row("Abnormal Totals", "${doubleToBC(score.abnormalPoints.filter{ it.side == Side.LEFT }.sumOf{it.value})}", "${doubleToBC(score.abnormalPoints.filter{ it.side == Side.RIGHT }.sumOf{it.value})}", "")
        writer.line()

        paint.isFakeBoldText = true
        writer.row("Gross Score", "", "", "${grossScoreText.text}")
        writer.row("Net Score", "", "", "${finalScoreText.text}")

        buckPic?.let { writer.image(it) }

        writer.finish()
        savePdfToDownloads(pdf)
    }


    private fun savePdfToDownloads(pdf: PdfDocument) {
        val filename = "Buck_Score_${System.currentTimeMillis()}.pdf"

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
            is MeasurementType.G -> "G${index}"
            is MeasurementType.AbnormalPoint -> "Abnormal ${index}"
            MeasurementType.MainBeam -> "Main Beam"
            is MeasurementType.Circumference -> "Circumference ${index}"
            MeasurementType.InnerSpread -> "Inner Spread"
            MeasurementType.GreatestSpread -> "Greatest Spread"
            MeasurementType.TipSpread -> "Tip to Tip Spread"
            MeasurementType.PointCount -> "Number of Points"
            MeasurementType.Width -> "Width"
        }
    }

    // ===============================
    // CLEARING / RESTORING
    // ===============================
    private fun clearScoreDisplay() {
        grossScoreText.text = "0"
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
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            eighthFractions
        )
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

    //I1
//    fun paired(type: MeasurementType): PairedMeasurement {
//        val left = measurements
//            .firstOrNull { it.type == type && it.side == Side.LEFT }?.value() ?:0.0
//
//        val right = measurements
//            .firstOrNull { it.type == type && it.side == Side.RIGHT }?.value() ?:0.0
//
//        return PairedMeasurement(type, left, right)
//    }
//
//    fun totalDifference(pairs: List<PairedMeasurement>): Double {
//        return pairs.sumOf { it.difference() }
//    }
//
//    fun leftSum(pairs: List<PairedMeasurement>): Double =
//        pairs.sumOf { it.left }
//
//    fun rightSum(pairs: List<PairedMeasurement>): Double =
//        pairs.sumOf { it.right }

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

}