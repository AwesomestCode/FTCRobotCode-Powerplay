package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.teamcode.subsystems.JunctionPositionSensor;
import org.firstinspires.ftc.teamcode.subsystems.MecanumDrivetrainMixer;

@TeleOp(group="Demo")
public class JunctionAlignmentDemo extends LinearOpMode {

    @Override
    public void runOpMode() throws InterruptedException {
        JunctionPositionSensor sensor = new JunctionPositionSensor(hardwareMap);

        DcMotorEx frontLeft = (DcMotorEx) hardwareMap.dcMotor.get("frontLeft");
        DcMotorEx frontRight = (DcMotorEx) hardwareMap.dcMotor.get("frontRight");
        DcMotorEx rearLeft = (DcMotorEx) hardwareMap.dcMotor.get("rearLeft");
        DcMotorEx rearRight = (DcMotorEx) hardwareMap.dcMotor.get("rearRight");

        MecanumDrivetrainMixer mixer = new MecanumDrivetrainMixer(frontLeft, frontRight, rearLeft, rearRight);

        waitForStart();

        while (opModeIsActive()) {
            int estimate = sensor.getEstimate();
            telemetry.addData("Estimate", estimate);
            if(gamepad1.touchpad) {
                mixer.setMovement(0, 0, estimate * 0.4);
            } else {
                mixer.setMovement(0, 0, 0);
            }
            telemetry.update();
            Thread.sleep(10);
        }
    }
}
