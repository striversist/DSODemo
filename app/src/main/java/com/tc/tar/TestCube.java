package com.tc.tar;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.primitives.Cube;

/**
 * Created by aarontang on 2017/5/8.
 */

public class TestCube extends Cube {

    public TestCube(float size) {
        super(size);

        Material material = new Material();
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.enableLighting(true);
        setMaterial(material);
    }
}
