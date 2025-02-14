package common.io.Encoding.encoder;

import common.io.Utils.FileConfig;

import java.io.ByteArrayOutputStream;

public class SinglePrecisionEncoderV1 extends GorillaEncoderV1{
    private int preValue;

    public SinglePrecisionEncoderV1() {
        // allowed do nothing
    }
    @Override
    public void encode(float value, ByteArrayOutputStream out) {
        if (!flag) {
            flag = true;
            preValue = Float.floatToIntBits(value);
            leadingZeroNum = Integer.numberOfLeadingZeros(preValue);
            tailingZeroNum = Integer.numberOfTrailingZeros(preValue);
            out.write(preValue & 0xFF);
            out.write((preValue >> 8) & 0xFF);
            out.write((preValue >> 16) & 0xFF);
            out.write((preValue >> 24) & 0xFF);
        } else {
            int nextValue = Float.floatToIntBits(value);
            int tmp = nextValue ^ preValue;
            if (tmp == 0) {
                // case: write '0'
                writeBit(false, out);
            } else {
                int leadingZeroNumTmp = Integer.numberOfLeadingZeros(tmp);
                int tailingZeroNumTmp = Integer.numberOfTrailingZeros(tmp);
                if (leadingZeroNumTmp >= leadingZeroNum && tailingZeroNumTmp >= tailingZeroNum) {
                    // case: write '10' and effective bits without first leadingZeroNum '0' and
                    // last tailingZeroNum '0'
                    writeBit(true, out);
                    writeBit(false, out);
                    writeBits(
                            tmp, out, FileConfig.VALUE_BITS_LENGTH_32BIT - 1 - leadingZeroNum, tailingZeroNum);
                } else {
                    // case: write '11', leading zero num of value, effective bits len and effective
                    // bit value
                    writeBit(true, out);
                    writeBit(true, out);
                    writeBits(leadingZeroNumTmp, out, FileConfig.LEADING_ZERO_BITS_LENGTH_32BIT - 1, 0);
                    writeBits(
                            FileConfig.VALUE_BITS_LENGTH_32BIT - leadingZeroNumTmp - tailingZeroNumTmp,
                            out,
                            FileConfig.FLOAT_VALUE_LENGTH - 1,
                            0);
                    writeBits(
                            tmp,
                            out,
                            FileConfig.VALUE_BITS_LENGTH_32BIT - 1 - leadingZeroNumTmp,
                            tailingZeroNumTmp);
                }
            }
            preValue = nextValue;
            leadingZeroNum = Integer.numberOfLeadingZeros(preValue);
            tailingZeroNum = Integer.numberOfTrailingZeros(preValue);
        }
    }

    @Override
    public void flush(ByteArrayOutputStream out) {
        encode(Float.NaN, out);
        clearBuffer(out);
        reset();
    }
    @Override
    public int getOneItemMaxSize() {
        // case '11'
        // 2bit + 5bit + 6bit + 32bit = 45bit
        return 6;
    }

    @Override
    public long getMaxByteSize() {
        // max(first 4 byte, case '11' bit + 5bit + 6bit + 32bit = 45bit) +
        // NaN(case '11' bit + 5bit + 6bit + 32bit = 45bit) = 90bit
        return 12;
    }
    private void writeBits(int num, ByteArrayOutputStream out, int start, int end) {
        for (int i = start; i >= end; i--) {
            int bit = num & (1 << i);
            writeBit(bit, out);
        }
    }

}
