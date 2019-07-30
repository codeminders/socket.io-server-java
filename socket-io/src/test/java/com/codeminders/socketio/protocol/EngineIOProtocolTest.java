package com.codeminders.socketio.protocol;

import com.codeminders.socketio.server.SocketIOProtocolException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EngineIOProtocolTest {
    @Test
    public void decodeMultiPayload()
            throws SocketIOProtocolException, StringIndexOutOfBoundsException
    {
        String payload = "10:40/stream,1:227:42/stream,[\"SET_STREAM_ID\"]1:1";
        List<EngineIOPacket> expected = new ArrayList<>();
        expected.add(new EngineIOPacket(EngineIOPacket.Type.MESSAGE, "0/stream,"));
        expected.add(new EngineIOPacket(EngineIOPacket.Type.PING, ""));
        expected.add(new EngineIOPacket(EngineIOPacket.Type.MESSAGE, "2/stream,[\"SET_STREAM_ID\"]"));
        expected.add(new EngineIOPacket(EngineIOPacket.Type.CLOSE, ""));

        List<EngineIOPacket> result = EngineIOProtocol.decodePayload(payload);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void decodeSimplePayload()
            throws SocketIOProtocolException
    {
        String payload = "10:40/stream,";
        List<EngineIOPacket> expected = new ArrayList<>();
        expected.add(new EngineIOPacket(EngineIOPacket.Type.MESSAGE, "0/stream,"));

        List<EngineIOPacket> result = EngineIOProtocol.decodePayload(payload);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    public void binaryEncode_unicodeStrings()
            throws IOException
    {
        final EngineIOPacket packet = new EngineIOPacket(EngineIOPacket.Type.MESSAGE, "Привет!");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EngineIOProtocol.binaryEncode(packet, baos);

        List<EngineIOPacket> result = EngineIOProtocol.binaryDecodePayload(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(packet);
    }
}
