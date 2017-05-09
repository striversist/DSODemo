package com.tc.tar;

import android.content.Context;
import android.opengl.GLES20;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.renderer.Renderer;

import java.util.Stack;

/**
 * Created by aarontang on 2017/5/8.
 */

public class DSORenderer extends Renderer {

    private RenderListener mRenderListener;
    private Object3D mCurrentCameraFrame;
    private float intrinsics[];

    public interface RenderListener {
        void onRender();
    }

    public DSORenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        intrinsics = TARNativeInterface.dsoGetIntrinsics();

        ArcballCamera arcball = new ArcballCamera(mContext, ((DSOActivity)mContext).getView());
        arcball.setPosition(0, -2, -5);
        arcball.setNearPlane(0.1);
        arcball.setFarPlane(1000);
        arcball.setFieldOfView(45);
        getCurrentScene().replaceAndSwitchCamera(getCurrentCamera(), arcball);

//        getCurrentScene().addChild(new TestCube(1.0f));
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        drawFrustum();
        if (mRenderListener != null) {
            mRenderListener.onRender();
        }
    }

    public void setRenderListener(RenderListener listener) {
        mRenderListener = listener;
    }

    private void drawFrustum() {
        float pose[] = TARNativeInterface.dsoGetCurrentPose();
        Matrix4 poseMatrix = new Matrix4();
        poseMatrix.setAll(pose);
        if (mCurrentCameraFrame == null) {
            mCurrentCameraFrame = createCameraFrame(0xff0000, 1);
            getCurrentScene().addChild(mCurrentCameraFrame);
        }
        mCurrentCameraFrame.setPosition(poseMatrix.getTranslation());
        mCurrentCameraFrame.setOrientation(new Quaternion().fromMatrix(poseMatrix));
    }

    private Line3D createCameraFrame(int color, int thickness) {
        float cx = intrinsics[0];
        float cy = intrinsics[1];
        float fx = intrinsics[2];
        float fy = intrinsics[3];
        int width = Constants.WIDTH;
        int height = Constants.HEIGHT;

        float sz = 0.1f;    // sizeFactor

        Stack<Vector3> points = new Stack<>();
        points.add(new Vector3(0, 0, 0));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (0 - cy) / fy, sz));
        points.add(new Vector3(0, 0, 0));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(0, 0, 0));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(0, 0, 0));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (0 - cy) / fy, sz));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (0 - cy) / fy, sz));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (height - 1 - cy) / fy, sz));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (0 - cy) / fy, sz));
        points.add(new Vector3(sz * (0 - cx) / fx, sz * (0 - cy) / fy, sz));
        points.add(new Vector3(sz * (width - 1 - cx) / fx, sz * (0 - cy) / fy, sz));

        Line3D frame = new Line3D(points, thickness, color);
        frame.setMaterial(new Material());
        frame.setDrawingMode(GLES20.GL_LINES);
        return frame;
    }
}
