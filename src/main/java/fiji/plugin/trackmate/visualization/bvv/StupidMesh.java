/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.visualization.bvv;

import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import bvv.core.backend.jogl.JoglGpuContext;
import bvv.core.shadergen.DefaultShader;
import bvv.core.shadergen.Shader;
import bvv.core.shadergen.generate.Segment;
import bvv.core.shadergen.generate.SegmentTemplate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import net.imglib2.mesh.impl.nio.BufferMesh;

public class StupidMesh
{
	private final Shader prog;

	private final BufferMesh mesh;

	private boolean initialized;

	private int vao;

	private final float[] carr = new float[ 4 ];

	private final float[] scarr = new float[ 4 ];

	public StupidMesh( final BufferMesh mesh )
	{
		this.mesh = mesh;
		final Segment meshVp = new SegmentTemplate( StupidMesh.class, "mesh.vp" ).instantiate();
		final Segment meshFp = new SegmentTemplate( StupidMesh.class, "mesh.fp" ).instantiate();
		prog = new DefaultShader( meshVp.getCode(), meshFp.getCode() );
		DisplaySettings.defaultStyle().getSpotUniformColor().getColorComponents( carr );
		DisplaySettings.defaultStyle().getHighlightColor().getColorComponents( scarr );
	}

	private void init( final GL3 gl )
	{
		initialized = true;

		final int[] tmp = new int[ 3 ];
		gl.glGenBuffers( 3, tmp, 0 );
		final int meshPosVbo = tmp[ 0 ];
		final int meshNormalVbo = tmp[ 1 ];
		final int meshEbo = tmp[ 2 ];

		final FloatBuffer vertices = mesh.vertices().verts();
		vertices.rewind();
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, meshPosVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, vertices.limit() * Float.BYTES, vertices, GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );

		final FloatBuffer normals = mesh.vertices().normals();
		normals.rewind();
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, meshNormalVbo );
		gl.glBufferData( GL.GL_ARRAY_BUFFER, normals.limit() * Float.BYTES, normals, GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );

		final IntBuffer indices = mesh.triangles().indices();
		indices.rewind();
		gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, meshEbo );
		gl.glBufferData( GL.GL_ELEMENT_ARRAY_BUFFER, indices.limit() * Integer.BYTES, indices, GL.GL_STATIC_DRAW );
		gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, 0 );

		gl.glGenVertexArrays( 1, tmp, 0 );
		vao = tmp[ 0 ];
		gl.glBindVertexArray( vao );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, meshPosVbo );
		gl.glVertexAttribPointer( 0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 0 );
		gl.glBindBuffer( GL.GL_ARRAY_BUFFER, meshNormalVbo );
		gl.glVertexAttribPointer( 1, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
		gl.glEnableVertexAttribArray( 1 );
		gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, meshEbo );
		gl.glBindVertexArray( 0 );
	}

	public void setColor( final Color color, final float alpha )
	{
		color.getComponents( carr );
		carr[ 3 ] = alpha;
	}

	public void setSelectionColor( final Color selectionColor, final float alpha )
	{
		selectionColor.getComponents( scarr );
		scarr[ 3 ] = alpha;
	}

	public void draw( final GL3 gl, final Matrix4fc pvm, final Matrix4fc vm, final boolean isSelected )
	{
		if ( !initialized )
			init( gl );

		final JoglGpuContext context = JoglGpuContext.get( gl );
		final Matrix4f itvm = vm.invert( new Matrix4f() ).transpose();

		prog.getUniformMatrix4f( "pvm" ).set( pvm );
		prog.getUniformMatrix4f( "vm" ).set( vm );
		prog.getUniformMatrix3f( "itvm" ).set( itvm.get3x3( new Matrix3f() ) );
		prog.getUniform4f( "ObjectColor" ).set( carr[ 0 ], carr[ 1 ], carr[ 2 ], carr[ 3 ] );
		prog.getUniform1f( "IsSelected" ).set( isSelected ? 1f : 0f );
		prog.getUniform4f( "SelectionColor" ).set( scarr[ 0 ], scarr[ 1 ], scarr[ 2 ], scarr[ 3 ] );
		prog.setUniforms( context );
		prog.use( context );

		gl.glBindVertexArray( vao );
		gl.glEnable( GL.GL_CULL_FACE );
		gl.glCullFace( GL.GL_BACK );
		gl.glFrontFace( GL.GL_CCW );
		gl.glDrawElements( GL_TRIANGLES, mesh.triangles().size() * 3, GL_UNSIGNED_INT, 0 );
		gl.glBindVertexArray( 0 );
	}
}
