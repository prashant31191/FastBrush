package co.adrianblan.fastbrush;

import android.content.Context;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;

import co.adrianblan.fastbrush.database.BristleParameters;
import co.adrianblan.fastbrush.globject.Bristle;
import co.adrianblan.fastbrush.globject.Brush;


/**
 * Class which computes physics with RenderScript
 */
public class PhysicsCompute {

    private RenderScript renderScript;
    private ScriptC_physics script;

    private Allocation inBristleIndices;
    private Allocation inAllocationTop;
    private Allocation inAllocationBottom;
    private Allocation outAllocation;

    ScriptField_ComputeParameters computeParameters;

    private Brush brush;

    float [] out;
    float [] inTop;
    float [] inBottom;

    public PhysicsCompute(Context context, Brush brush) {

        this.brush = brush;
        Bristle[] bristleArray = brush.getBristles();

        renderScript = RenderScript.create(context);
        script = new ScriptC_physics(renderScript);

        script.set_BRUSH_BASE_LENGTH(Bristle.BASE_LENGTH);
        script.set_SEGMENTS_PER_BRISTLE(Brush.SEGMENTS_PER_BRISTLE);
        script.set_script(script);

        int numBristles = brush.getBristles().length;
        int numSegments = Brush.SEGMENTS_PER_BRISTLE;

        int[] bristleIndices = new int[numBristles];
        for ( int i = 0; i < numBristles; i++) {
            bristleIndices[i] = i * 3;
        }

        inBristleIndices = Allocation.createSized(renderScript, Element.I32(renderScript), numBristles, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);
        inBristleIndices.copyFrom(bristleIndices);

        inTop = new float[3 * numBristles];
        inBottom  = new float[3 * numBristles];

        for(int i = 0; i < bristleArray.length; i++) {
            inTop[i * 3] = bristleArray[i].top.vector[0];
            inTop[i * 3 + 1] = bristleArray[i].top.vector[1];
            inTop[i * 3 + 2] = bristleArray[i].top.vector[2];

            inBottom[i * 3] = bristleArray[i].bottom.vector[0];
            inBottom[i * 3 + 1] = bristleArray[i].bottom.vector[1];
            inBottom[i * 3 + 2] = bristleArray[i].bottom.vector[2];
        }

        inAllocationTop = Allocation.createSized(renderScript, Element.F32(renderScript), 3 * numBristles, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);
        inAllocationBottom = Allocation.createSized(renderScript, Element.F32(renderScript), 3 * numBristles, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);

        inAllocationTop.copyFrom(inTop);
        inAllocationBottom.copyFrom(inBottom);

        outAllocation = Allocation.createSized(renderScript, Element.F32(renderScript), 3 * 2 * numSegments * numBristles, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);
        out = new float[3 * 2 * numSegments * numBristles];

        computeParameters = new ScriptField_ComputeParameters(renderScript, 1);

        script.bind_inBristlePositionTop(inAllocationTop);
        script.bind_inBristlePositionBottom(inAllocationBottom);
        script.bind_outBristlePosition(outAllocation);
        script.bind_computeParameters(computeParameters);
    }

    public float[] computeVertexData() {

        //Set all parameters for compute script
        computeParameters.set_brushPositionx(0, brush.getPosition().vector[0], false);
        computeParameters.set_brushPositiony(0, brush.getPosition().vector[1], false);
        computeParameters.set_brushPositionz(0, brush.getPosition().vector[2], false);

        BristleParameters bristleParameters = brush.getBristleParameters();

        computeParameters.set_planarDistanceFromHandle(0, bristleParameters.planarDistanceFromHandle, false);

        computeParameters.set_planarDistanceFromHandle(0, bristleParameters.planarDistanceFromHandle, false);
        computeParameters.set_upperControlPointLength(0, bristleParameters.upperControlPointLength, false);
        computeParameters.set_lowerControlPointLength(0, bristleParameters.lowerControlPointLength, false);

        computeParameters.set_sinHorizontalAngle(0, (float) Math.sin(Math.toRadians(brush.getHorizontalAngle())), false);
        computeParameters.set_cosHorizontalAngle(0, (float) Math.cos(Math.toRadians(brush.getHorizontalAngle())), false);

        computeParameters.copyAll();

        // Computes all positions
        script.invoke_compute(inBristleIndices);

        // Wait for script to complete before continuing
        renderScript.finish();

        outAllocation.copyTo(out);
        return out;
    }

    public void destroy() {
        inAllocationTop.destroy();
        inAllocationBottom.destroy();
        outAllocation.destroy();
        script.destroy();
        renderScript.destroy();
    }
}
