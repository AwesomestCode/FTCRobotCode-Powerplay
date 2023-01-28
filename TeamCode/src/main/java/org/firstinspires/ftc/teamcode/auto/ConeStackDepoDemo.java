package org.firstinspires.ftc.teamcode.auto;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.constraints.AngularVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.MecanumVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.MinVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.ProfileAccelerationConstraint;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.*;
import org.firstinspires.ftc.teamcode.auto.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.auto.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.subsystems.JunctionPositionSensor;
import org.firstinspires.ftc.teamcode.subsystems.SlidePositionSetter;
import org.firstinspires.ftc.teamcode.subsystems.SlidePositions;
import org.firstinspires.ftc.teamcode.subsystems.TapePositionSensor;

import java.util.Arrays;

@Autonomous(group="Demos")
public class ConeStackDepoDemo extends LinearOpMode {
    SampleMecanumDrive drive;

    public void rotate(double rotation) {
        drive.setWeightedDrivePower(new Pose2d(0, 0, rotation));
    }

    static int ZONE = 3;
    @Override
    public void runOpMode() throws InterruptedException {

        DigitalChannel led0 = hardwareMap.get(DigitalChannel.class, "led0");
        DigitalChannel led1 = hardwareMap.get(DigitalChannel.class, "led1");

        led0.setMode(DigitalChannel.Mode.OUTPUT);
        led1.setMode(DigitalChannel.Mode.OUTPUT);

        led0.setState(true);
        led1.setState(true);

        MultipleTelemetry multipleTelemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        TapePositionSensor tapeSensor = new TapePositionSensor(hardwareMap, TapePositionSensor.TapeColour.RED);

        telemetry.speak("Initialising. Please load cones");
        JunctionPositionSensor sensor = new JunctionPositionSensor(hardwareMap);
        CRServoImplEx intake = (CRServoImplEx) hardwareMap.get(CRServo.class, "intake");

        SlidePositionSetter slideSystem = new SlidePositionSetter(hardwareMap.get(DcMotorEx.class, "linearSlide1"), hardwareMap.get(DcMotorEx.class, "linearSlide2"), 20, false);

        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);

        this.drive = drive;

        // We want to start the bot at x: 10, y: -8, heading: 90 degree
        Pose2d startPose = new Pose2d(36, -62.5, Math.toRadians(90));

        drive.setPoseEstimate(startPose);

        Pose2d atConePose = new Pose2d(64, -12, Math.toRadians(0));

        TrajectorySequence clearSignal = drive.trajectorySequenceBuilder(startPose)
                .setConstraints(new MinVelocityConstraint(Arrays.asList(
                        new AngularVelocityConstraint(3.8603167394869375),
                        new MecanumVelocityConstraint(30, 14.4)
                )), new ProfileAccelerationConstraint(25))
                .forward(20 + 12)
                .setConstraints(new MinVelocityConstraint(Arrays.asList(
                        new AngularVelocityConstraint(3.8603167394869375),
                        new MecanumVelocityConstraint(50, 14.4)
                )), new ProfileAccelerationConstraint(45))
                .back(12)
                .forward(0.01)
                .resetConstraints()
                .build();

        TrajectorySequence getToJunction = drive.trajectorySequenceBuilder(clearSignal.end())
                .splineTo(new Vector2d(12 - 9, -36 + 9), Math.toRadians(AutoConstants.FIRST_JUNCTION_ROT))
                .waitSeconds(0.25)
                .build();

        TrajectorySequence goToStack1 = drive.trajectorySequenceBuilder(getToJunction.end())
                .setReversed(true)
                .splineTo(new Vector2d(10, -34), Math.toRadians(-90))
                .setReversed(false)
                .splineTo(new Vector2d(31, -12), Math.toRadians(0))
                .splineTo(new Vector2d(55, -12), Math.toRadians(0))
                //.lineToSplineHeading(new Pose2d(12, -30, Math.toRadians(0)))
                //.splineTo(new Vector2d(60, -12), Math.toRadians(-90))
                //.waitSeconds(0.5)
                .build();

        TrajectorySequence goForwardToStack = drive.trajectorySequenceBuilder(goToStack1.end())
                .lineTo(new Vector2d(68, -12))
                .build();

        TrajectorySequence goToJunction2 = drive.trajectorySequenceBuilder(atConePose)
                .back(6)
                //.lineToSplineHeading(new Pose2d(40, -12, Math.toRadians(180)))
                .addSpatialMarker(new Vector2d(48, -12), () -> slideSystem.setPosition(SlidePositions.TOP.getPosition()))
                .splineToSplineHeading(new Pose2d(36 - 9, -12 + 9, Math.toRadians(135)), Math.toRadians(135))
                .waitSeconds(0.5)
                .build();

        DetectAprilTagZoneUtil.initialise(hardwareMap, telemetry);

        waitForStart();

        telemetry.speak("Stand back drivers. I have commenced the demo.");

        ZONE = DetectAprilTagZoneUtil.getZone(hardwareMap, telemetry);

        telemetry.speak("I have detected the zone as " + ZONE);
        telemetry.clearAll();
        telemetry.addLine("Finished detecting zone, going to junction");
        telemetry.update();

        led0.setState(false);

        intake.setPower(0.5);
        drive.followTrajectorySequence(clearSignal);
        intake.setPower(0.3);
        slideSystem.setPosition(SlidePositions.TOP.getPosition());
        led0.setState(true);

        drive.followTrajectorySequence(getToJunction);
        telemetry.clearAll();
        telemetry.addLine("At junction, attempting alignment");
        telemetry.update();
        sensor.align(this::rotate);
        telemetry.speak("Finished re-aligning");
        slideSystem.setPosition(SlidePositions.TOP.getPosition() - 200);
        sleep(250);
        intake.setPower(-1);
        sleep(500);

        //drive.followTrajectorySequence(returnToOrigin);
        intake.setPower(0);
        slideSystem.setPosition(SlidePositions.LOW.getPosition() - 100);
        intake.setPower(0.3);
        sleep(200);
        intake.setPower(0);

        telemetry.clearAll();
        telemetry.addLine("Deposited, going to stack");
        telemetry.update();

        drive.followTrajectorySequence(goToStack1);

        slideSystem.setPosition(SlidePositions.WALL.getPosition());

        telemetry.clearAll();
        telemetry.addLine("At stack, attempting to align");
        telemetry.update();

        telemetry.speak("Attempting to align");
        boolean spoken = false;

        while(!tapeSensor.isInRange()) {
            if(!spoken) telemetry.speak("Sensor not in range");
            spoken = true;
            drive.setWeightedDrivePower(new Pose2d(0, -0.4, 0));
            multipleTelemetry.update();
            //telemetry.update();
        }

        telemetry.clearAll();
        telemetry.addLine("In Range");
        telemetry.update();

        if(spoken) telemetry.speak("Now in range");
        telemetry.speak("Already in range");
        doAlignment(multipleTelemetry, tapeSensor, drive);
        telemetry.addLine("Alignment successful");
        telemetry.update();

        drive.setPoseEstimate(new Pose2d(55, -12));

        telemetry.addLine("Trying to get to stack");
        telemetry.update();

        drive.followTrajectorySequence(goForwardToStack);

        telemetry.speak("Attempting to align");
        spoken = false;

        while (!tapeSensor.isInRange()) {
            if (!spoken) telemetry.speak("Sensor not in range");
            spoken = true;
            drive.setWeightedDrivePower(new Pose2d(0, -0.4, 0));
            multipleTelemetry.update();
            //telemetry.update();
        }

        telemetry.clearAll();
        telemetry.addLine("In Range");
        telemetry.update();

        doAlignment(multipleTelemetry, tapeSensor, drive);

        slideSystem.setPosition(SlidePositions.WALL.getPosition() - 250);
        intake.setPower(0.3);
        sleep(750);
        slideSystem.setPosition(SlidePositions.WALL.getPosition() + 500);
        led1.setState(false);
        sleep(250);
        led1.setState(true);

        drive.setPoseEstimate(atConePose);

        drive.followTrajectorySequence(goToJunction2);
        telemetry.clearAll();
        telemetry.addLine("At junction, attempting alignment");
        telemetry.update();
        sensor.align(this::rotate);
        telemetry.speak("Finished re-aligning");
        slideSystem.setPosition(SlidePositions.TOP.getPosition() - 200);
        sleep(250);
        intake.setPower(-0.3);
        sleep(1000);






        //drive.setPoseEstimate(parkStart);

        if(ZONE == 1) {
            //drive.followTrajectorySequence(goLeft);
        } else if(ZONE == 3) {
            //drive.followTrajectorySequence(goRight);
        }

        //drive.followTrajectorySequence(goToStack1);
        //slideSystem.setPosition(100);
        //sleep(1000);
        //drive.followTrajectorySequence(goToJunction2);


    }

    private void doAlignment(MultipleTelemetry multipleTelemetry, TapePositionSensor tapeSensor, SampleMecanumDrive drive) {
        tapeSensor.align((double strafeAmount) -> {
            drive.setWeightedDrivePower(new Pose2d(0, strafeAmount * -AutoConstants.TAPE_STRAFE_COEFFICIENT, 0));
            multipleTelemetry.addData("Strafe", strafeAmount * AutoConstants.TAPE_STRAFE_COEFFICIENT);
            multipleTelemetry.update();
        }, multipleTelemetry);

        telemetry.speak("Aligned successfully");


        telemetry.clearAll();
    }
}
