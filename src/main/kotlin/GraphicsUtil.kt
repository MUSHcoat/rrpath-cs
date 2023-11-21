import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.acmerobotics.roadrunner.path.Path
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle


object GraphicsUtil {
    val DEFUALT_RESOLUTION = 2 // inches

    val FIELD_WIDTH = 144.0 // 12'

    val ROBOT_WIDTH = 14.0
    val ROBOT_HEIGHT = 14.0

    val LINE_THICKNESS = 3.0

    val PATH_COLOR = Color.YELLOW
    val ROBOT_COLOR = Color.MAROON
    val OUTTAKE_PIVOT_COLOR = Color.BLUE;
    val OUTTAKE_ARM_COLOR = Color.BISQUE;
    val ROBOT_VECTOR_COLOR = Color.BLUE
    val END_BOX_COLOR = Color.GREEN

    lateinit var gc: GraphicsContext

    fun setColor(color: Color) {
        gc.stroke = color
        gc.fill = color
    }

    fun drawSampledPath(path: Path) {
        setColor(PATH_COLOR)
        val samples = Math.ceil(path.length() / DEFUALT_RESOLUTION).toInt()
        val points = Array(samples) { Vector2d() }
        val dx = path.length() / (samples - 1).toDouble()
        for (i in 0 until samples) {
            val displacement = i * dx
            val pose = path[displacement]
            points[i] = pose.vec()
        }
        strokePolyline(points)
    }

    fun drawRobotTrail(path: Path) {
        val samples = Math.ceil(path.length() / DEFUALT_RESOLUTION).toInt() * 75
        val dx = path.length() / (samples - 1).toDouble()
        for (i in 0 until samples) {
            val displacement = i * dx
            val pose = path[displacement]
            drawRectangleRotated(pose.vec().toPixel.x - ROBOT_WIDTH * pixelsPerInch / 2, pose.vec().toPixel.y - ROBOT_WIDTH * pixelsPerInch / 2, Math.toDegrees(-pose.heading), ROBOT_WIDTH * pixelsPerInch, ROBOT_WIDTH * pixelsPerInch, Color.GREEN)
        }
    }

    fun strokePolyline(points: Array<Vector2d>) {
        val pixels = points.map { it.toPixel }
        gc.strokePolyline(pixels.map { it.x }.toDoubleArray(), pixels.map { it.y }.toDoubleArray(), points.size)
    }

    fun strokeLine(p1: Vector2d, p2: Vector2d) {
        val pix1 = p1.toPixel
        val pix2 = p2.toPixel
        gc.strokeLine(pix1.x, pix1.y, pix2.x, pix2.y)
    }

    fun drawRobotVector(pose2d: Pose2d) {
        gc.globalAlpha = 0.75

        val point1 = pose2d.vec()
        val v = pose2d.headingVec() * ROBOT_WIDTH / 2.0
        val point2 = point1 + v

        setColor(ROBOT_VECTOR_COLOR)
        strokeLine(point1, point2)

        gc.globalAlpha = 0.75
    }

    fun fillRect(center: Vector2d, w: Double, h: Double) {
        val center_pix = center.toPixel
        val pix_w = w * pixelsPerInch
        val pix_h = h * pixelsPerInch

        gc.fillRect(center_pix.x - pix_w / 2.0, center_pix.y - pix_h / 2.0, pix_w, pix_h)
    }

    fun drawRectangleRotated(x: Double, y: Double, r: Double, w: Double, h: Double, fill: Paint) {
        val rotationCenterX: Double = x + (w / 2)
        val rotationCenterY: Double = y + (h / 2)

        gc.save()
        gc.translate(rotationCenterX, rotationCenterY);
        gc.rotate(r);
        val oldFill = gc.fill
        gc.fill = fill
        gc.fillRect(-w / 2, -h / 2, w, h)
        gc.fill = oldFill
        gc.translate(-rotationCenterX, -rotationCenterY);
        gc.restore()
    }

    fun updateRobotRect(rectangle: Rectangle, pose2d: Pose2d, color: Color, opacity: Double) {
        val pix_w = ROBOT_WIDTH * pixelsPerInch
        val pix_h = ROBOT_HEIGHT * pixelsPerInch

        rectangle.width = pix_w
        rectangle.height = pix_h

        val center_pix = pose2d.vec().toPixel
        rectangle.x = center_pix.x - pix_w / 2.0
        rectangle.y = center_pix.y - pix_h / 2.0
        rectangle.fill = color
        rectangle.opacity = opacity
        rectangle.rotate = Math.toDegrees(-pose2d.heading)
    }

    val pivot = Pose2d(-5.51, 0.0, Math.toRadians(180.0))
    val targetPosition = Vector2d(0.0, -24.0)

    fun updateOuttakePivotRect(rectangle: Rectangle, pose2d: Pose2d, color: Color, opacity: Double) {
        val inches = 2.5
        val pix_w = inches * pixelsPerInch
        val pix_h = inches * pixelsPerInch

        rectangle.width = pix_w
        rectangle.height = pix_h

        val realPos = Pose2d(pose2d.vec().plus(pivot.vec().rotated(pose2d.heading)), pose2d.heading + pivot.heading)

        val center_pix = realPos.vec().toPixel
        rectangle.x = center_pix.x - pix_w / 2.0
        rectangle.y = center_pix.y - pix_h / 2.0
        rectangle.fill = color
        rectangle.opacity = opacity
        rectangle.rotate = Math.toDegrees(-realPos.heading)
    }

    val arm_length = 11.789;
    val ArmPose = Pose2d(11.789 / 2.0, 0.0, Math.toRadians(0.0))

    fun updateOuttakeArmRect(rectangle: Rectangle, pose2d: Pose2d, color: Color, opacity: Double) {
        val pix_w = 2 * pixelsPerInch
        val pix_h = 11.789 * pixelsPerInch

        rectangle.width = pix_w
        rectangle.height = pix_h

        val turretWorldPose = Pose2d(
            pose2d.vec().plus(pivot.vec().rotated(pose2d.heading)),
            pose2d.heading + pivot.heading
        )

        val turretAngle: Double = targetPosition.minus(turretWorldPose.vec()).angle() - turretWorldPose.heading

        val realPos = Pose2d(pose2d.vec().plus(pivot.vec().rotated(pose2d.heading)).plus(ArmPose.vec().rotated(pose2d.heading + pivot.heading + turretAngle)), pose2d.heading + pivot.heading + turretAngle)

        val center_pix = realPos.vec().toPixel
        rectangle.x = center_pix.x - pix_w / 2.0
        rectangle.y = center_pix.y - pix_h / 2.0
        rectangle.fill = color
        rectangle.opacity = opacity
        rectangle.rotate = Math.toDegrees(-realPos.heading)
    }

    var pixelsPerInch = 0.0
    var halfFieldPixels = 0.0
}


val Vector2d.toPixel
    get() = Vector2d(
        -y * GraphicsUtil.pixelsPerInch + GraphicsUtil.halfFieldPixels,
        -x * GraphicsUtil.pixelsPerInch + GraphicsUtil.halfFieldPixels
    )