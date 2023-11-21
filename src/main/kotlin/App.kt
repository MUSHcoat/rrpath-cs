import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font

import javafx.stage.Stage
import javafx.util.Duration
import kotlin.random.Random

class App(private val TEXT_SIZE: Double = 20.0, private val TEXT_AREA_HEIGHT: Double = 100.0) : Application() {
    private val robotRect = Rectangle(0.0, 0.0, 10.0, 10.0)
    private val outtakePivotRect = Rectangle(0.0, 0.0, 2.0, 2.0)
    private val outtakeArmRect = Rectangle(0.0, 0.0, 11.789, 3.0)
    private val startRect = Rectangle(0.0, 0.0, 10.0, 10.0)
    private val endRect = Rectangle(0.0, 0.0, 10.0, 10.0)
    private var canvas: Canvas = Canvas(100.0, 100.0)
    private var durationText = "Total time: %.2f"
    private val durationLabel = Label(durationText.format(0.0))
    private var currentTimeText = "Current time: %.2f"
    private val currentTimeLabel = Label(currentTimeText.format(0.0))
    private var trajectoryDurationText = "Trajectory time: %.2f"
    private val trajectoryDurationLabel = Label(trajectoryDurationText.format(0.0))
    private var currentTrajectoryTimeText = "Current trajectory time: %.2f"
    private val currentTrajectoryTimeLabel = Label(currentTrajectoryTimeText.format(0.0))
    private var scrubbingSpeedText = "Scrubbing speed: %.5f"
    private val scrubbingSpeedLabel = Label(scrubbingSpeedText.format(0.0))

    private var startTime = Double.NaN
    private val trajectories = TrajectoryGen.createTrajectory()

    private lateinit var fieldImage: Image
    private lateinit var stage: Stage

    private val trajectoryDurations = trajectories.map { it.duration() }
    private val duration = trajectoryDurations.sum()
    private val numberOfTrajectories = trajectories.size

    private var scrubbingSpeed = 1.0

    private fun setFieldSize(size: Double) {
        canvas.width = size
        canvas.height = size
        GraphicsUtil.pixelsPerInch = size / GraphicsUtil.FIELD_WIDTH
        GraphicsUtil.halfFieldPixels = size / 2.0
    }

    override fun start(stage: Stage?) {
        this.stage = stage!!
        fieldImage = Image("/CSdiagram.png")

        val canvasGroup = Group()

        setFieldSize(fieldImage.width)

        val gc = canvas.graphicsContext2D
        val t1 = Timeline(KeyFrame(Duration.millis(10.0), EventHandler<ActionEvent> { run(gc) }))
        t1.cycleCount = Timeline.INDEFINITE

        val grid = GridPane()
        grid.hgap = 5.0
        grid.add(canvasGroup, 0, 0, 3, 1)
        grid.add(durationLabel, 0, 1)
        grid.add(currentTimeLabel, 0, 2)
        grid.add(trajectoryDurationLabel, 2, 1)
        grid.add(currentTrajectoryTimeLabel, 2, 2)
        grid.add(scrubbingSpeedLabel, 1, 1)

        stage.scene = Scene(
            grid
        )

        stage.scene.onKeyPressed = EventHandler<KeyEvent> {
            when (it.code) {
                KeyCode.SPACE -> paused = !paused
                KeyCode.LEFT -> runtime -= scrubbingSpeed
                KeyCode.RIGHT -> runtime += scrubbingSpeed
                KeyCode.ADD -> {
                    if (scrubbingSpeed < duration)
                        scrubbingSpeed *= 2
                }
                KeyCode.SUBTRACT-> {
                    if (scrubbingSpeed > 1e-5)
                        scrubbingSpeed /= 2
                }
                else -> {}
            }
        }

        canvasGroup.children.addAll(canvas, startRect, endRect, robotRect, outtakePivotRect, outtakeArmRect)

        stage.title = "PathVisualizer"
        stage.isResizable = true
        stage.widthProperty().addListener { _, _, newW -> setFieldSize(Math.min(newW.toDouble(), stage.height - TEXT_AREA_HEIGHT) - 17) }
        stage.heightProperty().addListener { _, _, newH -> setFieldSize(Math.min(newH.toDouble() - TEXT_AREA_HEIGHT, stage.width) - 17) }
        stage.width = 700.0
        stage.height = stage.width + TEXT_AREA_HEIGHT

        durationLabel.font = Font("Arial", TEXT_SIZE)
        currentTimeLabel.font = Font("Arial", TEXT_SIZE)
        trajectoryDurationLabel.font = Font("Arial", TEXT_SIZE)
        currentTrajectoryTimeLabel.font = Font("Arial", TEXT_SIZE)
        scrubbingSpeedLabel.font = Font("Arial", TEXT_SIZE)

        grid.prefWidthProperty().bind(stage.widthProperty())
        val columnConstraints = ColumnConstraints(100.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE)
        columnConstraints.hgrow = Priority.ALWAYS
        grid.columnConstraints.addAll(columnConstraints, columnConstraints, columnConstraints)

        durationLabel.text = durationText.format(duration)
        currentTimeLabel.text = currentTimeText.format(0.0)

        stage.show()
        t1.play()
    }

    private var runtime = 0.0
    private var prevTime = Clock.seconds

    private var paused = false
    private var prevPaused = false

    private fun run(gc: GraphicsContext) {
        val deltaTime = Clock.seconds - prevTime
        prevTime = Clock.seconds
        if (!paused) {
            runtime += deltaTime
        } else {
            if (!prevPaused) {
                trajectories.forEach { GraphicsUtil.drawRobotTrail(it.path) }
                trajectories.forEach { GraphicsUtil.drawSampledPath(it.path) }
            }
        }
        prevPaused = paused
        if(paused) {
            return
        }
        if (startTime.isNaN())
            startTime = Clock.seconds

        GraphicsUtil.gc = gc
        gc.drawImage(fieldImage, 0.0, 0.0, canvas.width, canvas.height)

        gc.lineWidth = GraphicsUtil.LINE_THICKNESS

        gc.globalAlpha = 0.5
        GraphicsUtil.setColor(Color.RED)
        TrajectoryGen.drawOffbounds()
        gc.globalAlpha = 1.0

        while (runtime >= duration)
            runtime -= duration
        while (runtime < 0)
            runtime += duration

        var activeTrajectoryIndex = 0
        var prevDurations = 0.0
        while (activeTrajectoryIndex < numberOfTrajectories && prevDurations + trajectoryDurations[activeTrajectoryIndex] < runtime) {
            prevDurations += trajectoryDurations[activeTrajectoryIndex]
            activeTrajectoryIndex++
        }

        val trajectory = trajectories[activeTrajectoryIndex]

        val profileTime = runtime - prevDurations

        val start = trajectories.first().start()
        val end = trajectories.last().end()
        val current = trajectory[profileTime]

        trajectories.forEach { GraphicsUtil.drawSampledPath(it.path) }

        GraphicsUtil.updateRobotRect(startRect, start, GraphicsUtil.END_BOX_COLOR, 0.5)
        GraphicsUtil.updateRobotRect(endRect, end, GraphicsUtil.END_BOX_COLOR, 0.5)

        GraphicsUtil.updateOuttakeArmRect(outtakeArmRect, current, GraphicsUtil.OUTTAKE_ARM_COLOR, 0.85)
        GraphicsUtil.updateOuttakePivotRect(outtakePivotRect, current, GraphicsUtil.OUTTAKE_PIVOT_COLOR, 0.85)
        GraphicsUtil.updateRobotRect(robotRect, current, GraphicsUtil.ROBOT_COLOR, 0.75)
        GraphicsUtil.drawRobotVector(current)

        currentTimeLabel.text = currentTimeText.format(runtime)
        trajectoryDurationLabel.text = trajectoryDurationText.format(trajectoryDurations[activeTrajectoryIndex])
        currentTrajectoryTimeLabel.text = trajectoryDurationText.format(profileTime)
        scrubbingSpeedLabel.text = scrubbingSpeedText.format(scrubbingSpeed)
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}