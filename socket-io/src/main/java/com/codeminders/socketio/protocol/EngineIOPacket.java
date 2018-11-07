/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.protocol;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class EngineIOPacket
{
    public enum Type
    {
        OPEN(0),
        CLOSE(1),
        PING(2),
        PONG(3),
        MESSAGE(4),
        UPGRADE(5),
        NOOP(6),
        UNKNOWN(-1);

        private int value;

        Type(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }

        public static Type fromInt(int i)
        {
            switch (i)
            {
                case 0: return OPEN;
                case 1: return CLOSE;
                case 2: return PING;
                case 3: return PONG;
                case 4: return MESSAGE;
                case 5: return UPGRADE;
                case 6: return NOOP;
                default:
                    return UNKNOWN;
            }
        }
    }

    private Type        type;
    private String      textData;
    private InputStream binaryData;

    public Type getType()
    {
        return type;
    }

    public String getTextData()
    {
        return textData;
    }

    public InputStream getBinaryData()
    {
        return binaryData;
    }

    public EngineIOPacket(Type type, String data)
    {
        this.type = type;
        this.textData = data;
    }

    //TODO: support byte[] in addtion to InputStream
    public EngineIOPacket(Type type, InputStream binaryData)
    {
        this.type = type;
        this.binaryData = binaryData;
    }

    public EngineIOPacket(Type type)
    {
        this(type, "");
    }

    @Override
    public String toString()
    {
        return "EngineIOPacket{" +
                "type=" + type +
                ", textData='" + textData + '\'' +
                ", binaryData=" + binaryData +
                '}';
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineIOPacket that = (EngineIOPacket) o;
        return type == that.type &&
                Objects.equals(textData, that.textData) &&
                Objects.equals(binaryData, that.binaryData);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, textData, binaryData);
    }
}
