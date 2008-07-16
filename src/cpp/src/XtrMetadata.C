/*
* Copyright (c) 2005,2006,2007 The Regents of the University of California.
* All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#include <cctype>
#include <cstdio>
#include <ctime>
#include <sys/types.h>
#include <unistd.h>
#include "XtrMetadata.h"

namespace xtr {
/* util function */
int
xtr_hex_to_int(char c)
{
   char uc;

   uc = toupper(c);

   if ('0' <= uc && uc <= '9') {
      return uc - '0';
   } else if ('A' <= uc && uc <= 'F') {
      return ((uc - 'A') + 10);
   } else {
      /* shouldn't be here */
      assert(0);
   }
   return 0;
};

/* This function will write to *p and *(p+1). 
   It will not check p for nullness.
   It will not null-terminate the result
*/
   
void
xtr_btoh(u_int8_t b, char *p) {
    const static char h[16] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    *(p++) = h[b >> 4];
    *p     = h[b & 0xF];
     
}

/*********************************************************/

OpId::OpId(const u_int8_t* from, size_t len)
    : Id<XTR_MAX_OP_ID_LEN>(4)
{
    assert(from);
    if (isValidLength(len)) {
        length = len;
        memset(id, 0, sizeof(id));
        memcpy(id, from, len);
    }
}

OpId::OpId(size_t len) 
    : Id<XTR_MAX_OP_ID_LEN>(len) 
{
    if (!isLengthValid())
        setLength(4);
}   

/*********************************************************/

TaskId::TaskId(size_t len) 
    : Id<XTR_MAX_TASK_ID_LEN>(len) 
{
    if (!isLengthValid())
        setLength(4);
}   

TaskId::TaskId(const u_int8_t* from, size_t len) 
    :Id<XTR_MAX_TASK_ID_LEN>(4)
{
    assert(from);
    if (isValidLength(len)) {
        length = len;
        memset(id, 0, sizeof(id));
        memcpy(id, from, len);
    }
}


bool
TaskId::isValid() const
{
    unsigned int i;
    int invalid = 1;
    if (!isLengthValid())
        return 0;
    for (i = 0; i < length && invalid; i++) 
        invalid &= (id[i] == 0);
    return !(invalid);
}

/*********************************************************/
/** Create Options from a byte array, reading each options
 *  in succession. The format is that in the Metadata
 *  specification. 
 *  @param from byte array to read options from
 *  @param len  the length of the options in bytes. Differently
 *         from other similar functions, this functions reads
 *         all the bytes in len, padding with no-ops after the
 *         first no-op or an error is encountered in parsing
 *  
 */

Options::Options(const u_int8_t *from, u_int8_t len)
    :  reserved_count(0), count(0), length(0), opts(0)
{
    const u_int8_t* p = from;
    u_int8_t remain = len;
    size_t opt_size;
    u_int8_t opt_count = 0;
    OptionAny opt;
    Option **opt_p;
  
    if (!from) 
        return;

    //loop to count the options. We need to do
    //this because we need to allocate the right
    //number of options.

    do {
        opt_size = remain; 
        opt.initFromBytes(p, &opt_size);
        if (!opt_size)
            break;
        remain -= opt_size;
        p += opt_size;
        opt_count++;
    } while (remain && opt.getType() != 0);    
    
    if (remain > 0) {
        //this happens if:
        //  1. the first no-op was found, in which case
        //     all other options must be no-ops
        //  2. an error occurred parsing an options
        //In both cases we fill the rest of the space with
        //  no-ops, which are 1-byte each.
        opt_count+=remain; 
    }

    //allocate the new space
    reserve(opt_count);
    count = opt_count;
    length = len;

    // loop to store the options.
    p = from;
    remain = len; 
    opt_p = opts - 1;
    do {
        opt_p++;
        opt_size = remain;
        *opt_p = Option::createFromBytes(p, &opt_size); 
        if (!opt_size)
            break;
        remain -= opt_size;
        p += opt_size;
    } while(remain && (*opt_p)->getType() != 0);

    if (remain > 0) {
        assert(opt_p - opts + 1 + remain == count);
    }
    while (remain) {
        remain--;
        opt_p++;
        *opt_p = new OptionNop();
    }
}

Options::Options(const Options& other) 
    :reserved_count(0), count(0), length(0), opts(0) 
{
    int i;
    reserve(other.reserved_count);
    count = other.count;
    length = other.length;
    for (i = 0; i < count; i++) {
        opts[i] = other.opts[i]->clone();
    }
}

Options&
Options::operator=(const Options& other)
{
    int i;
    //prevent self assignment
    if (this != &other) {
        clear();
        reserve(other.reserved_count);
        count = other.count;
        length = other.length;
        for (i = 0; i < count; i++) {
            opts[i] = other.opts[i]->clone();
        } 
    }
    return *this;
}

Options::~Options() 
{
    clear();
}

void
Options::clear() 
{
    int i;
    assert(count <= reserved_count);
    if (reserved_count) {
        for (i = 0; i < count; i++) {
            assert(opts[i]);
            delete opts[i];
        }
        delete[] opts;
        reserved_count = 0;
    }
    count = 0;
    length = 0;
}

xtr_result
Options::reserve(u_int8_t c) {
    Option **o;
    if (c <= reserved_count)
        return XTR_SUCCESS;
    if (reserved_count) {
        o = opts;
        opts = new Option*[c];
        memcpy(opts, o, count*sizeof(Option*));
        delete[] o; 
    } else {
        opts = new Option*[c];
    }
    reserved_count = c;
    return XTR_SUCCESS;
}



xtr_result
Options::addOption(const Option &option) 
{
    u_int8_t space_left = 255 - length;
    u_int8_t reserve_left = 255 - reserved_count;
    u_int8_t to_reserve;
    
    u_int8_t *b;
    xtr_result r = XTR_FAIL;
    size_t s;
    Option* o = 0;    

    enum {
        INC= 5
    };
    if (space_left < option.getLength())
        return XTR_FAIL;
    if (reserved_count - count < 1) {
        to_reserve = (reserve_left < INC)?
            reserve_left:INC;
        reserve(reserved_count + to_reserve);
    }

    /* normalize option */
    if (dynamic_cast<const OptionAny*>(&option)) {
        b = new u_int8_t[option.getSize()];
        r = option.pack(b,&(s=option.getSize()));
        if (r == XTR_SUCCESS && s) {
            o = Option::createFromBytes(b,&s);
            if (s) {
                opts[count++] = o;
                length += o->getSize();
            } else {
                delete o;
            }
        }
        delete[] b;
        if (r == XTR_FAIL || !s)
            return XTR_FAIL;
    } else {
        opts[count++] = option.clone();
        length += option.getSize();
    }
    return XTR_SUCCESS; 
}

xtr_result
Options::removeOptionAt(u_int8_t i) 
{
    int j;
    if (i >= count) 
        return XTR_FAIL;
    count--;
    length -= opts[i]->getSize();
    delete opts[i];
    for (j = i; j < count; j++) {
        opts[j] = opts[j+1];
    }
    opts[count] = 0;
    return XTR_SUCCESS;
}

xtr_result
Options::pack(u_int8_t *dest, size_t *size) const
{
    u_int8_t* p = dest;
    u_int8_t i;
    size_t s;
    size_t remain;
    if (!dest || !size)
        return XTR_FAIL;
    if (*size < length) 
        return XTR_FAIL;
    remain = *size;
    for (i = 0; i < count; i++) {
        s = remain;     
        opts[i]->pack(p, &s);
        p += s;
        remain -= s;
    }
    *size = *size - remain;
    return XTR_SUCCESS;
} 

bool 
Options::isEqual(const Options& other) const 
{
    bool equal = 1;
    int i;
    if (count != other.count)
        return 0;
    if (length != other.length)
        return 0;
    for (i = 0; i < count && equal; i++)
        equal &= opts[i]->isEqual(*other.opts[i]);
    return equal;    
}

/****************************************************/

void 
Metadata::initRandom() 
{
    static bool random_init = 0;
    if (!random_init) {
        srand((unsigned)time(0) ^ (u_int32_t)getpid());
        random_init = 1;
    }
}


/* Private constructor used by createFromString and createFromBytes */
Metadata::Metadata(create_t t, const void *from, size_t len)
    : version(XTR_CURRENT_VERSION), taskId(), opId(), options()
{
    const u_int8_t *p;
    size_t remain;
    u_int8_t *packed = 0; 

    size_t l;
    u_int8_t flags;
    u_int8_t task_id_len;
    u_int8_t op_id_len;
    u_int8_t opts_len;
    bool has_options;

    initRandom();

    if (!from)
        return;
    /* If a string, first convert to a binary representation */
    if (t == CREATE_FROM_STRING) {
        unsigned int i;
        const char* from_s;
        size_t packed_length = 0;

        from_s = static_cast<const char*>(from);

        /* Not handling wider char values for now */
        assert(sizeof(char) == 1);
    
        /* len has to be even */
        if (len % 2) 
            return;

        packed_length = (len) / 2;
        if (packed_length < XTR_MD_MIN_LENGTH)
            return;

        /* Convert the string to the binary representation */
        packed = new u_int8_t[packed_length];
        for (i = 0; i < packed_length; i++) {
            if (!isxdigit(from_s[2*i]) || !isxdigit(from_s[2*i + 1]))
                break;
            packed[i] = ((xtr_hex_to_int(from_s[2*i]) & 0x0f) << 4) |
                         (xtr_hex_to_int(from_s[2*i + 1]) & 0x0f);
        }

        if (i < packed_length) {
           /* an error occurred, return an invalid id */
           delete[] packed;
           return;
        }
        p = packed;
        remain = packed_length;
    } 
    /* If binary, just do it */
    else if (t == CREATE_FROM_BYTES) {
        if (len < XTR_MD_MIN_LENGTH)
            return;
        p = static_cast<const u_int8_t*>(from);
        remain = len;
    } else {
        return;
    }

    /* Perform the binary conversion */

    //flags
    flags = *p;
    version = (flags & XTR_MASK_VERSION) >> 4;
    op_id_len = (flags & XTR_MASK_OPIDLEN) >> 3;
 
 
    //version must be 0 or 1 in this implementation
    //According to the spec, version 0 can't have the
    //opIdLen flag set to 1.
    //we will be liberal and try to read version 0 even
    //if it has the opIdLen flag set to 1. In this case
    //we will serialize the resulting metadata as version 1,
    //though.
    if (version  > XTR_CURRENT_VERSION) {
        version = 0;
        if (packed)
            delete[] packed;
        return;
    }
    
    if (op_id_len)
        version = 1;
    
    has_options = ((flags & XTR_MASK_HASOPTION) != 0);
    task_id_len = (flags & XTR_MASK_IDLEN); // >> 0

    p++; remain--;
    //taskId
    switch(task_id_len) {
    case 0:
        l = 4;
        break;
    case 1:
        l = 8;
        break;
    case 2:
        l = 12;
        break;
    case 3:
        l = 20;
        break;
    default:
        l = 0; //can't happen
    }

    //taskId
    if (remain < l) {
        if (packed)
            delete[] packed;
        return;
    }
    taskId = TaskId(p, l);
    p += l; remain -= l;

    //opId
    if (op_id_len == 0) 
        l = 4;
    else
        l = 8;
        
    if (remain < l) {
        clear();
        if (packed)
            delete[] packed;
        return;
    }
    opId = OpId(p, l);
    p += l; remain -= l;

    //options
    if (has_options) {
        if (!remain) {
            clear();   
            if (packed)
                delete[] packed;
            return;
        }
        opts_len = *p;
        p++; remain--;
        if (opts_len == 0) {
            if (packed)
                delete[] packed;
            return;
        }
        if (remain < opts_len) {
            clear();   
            if (packed)
                delete[] packed;
            return;
        }
        options = Options(p, opts_len);
        assert(options.getLength() == opts_len);
        remain -= opts_len;
    }

    if (packed)
        delete[] packed;
}

/* Private constructor used by createRandom */
Metadata::Metadata(create_t t, size_t taskIdLen, size_t opIdLen)
    : version(XTR_CURRENT_VERSION), taskId(), opId(), options()
{
    initRandom();
    if (t == CREATE_RANDOM &&
        taskId.isValidLength(taskIdLen) &&
        opId.isValidLength(opIdLen)) 
    {
        setRandomTaskId(taskIdLen);
        setRandomOpId(opIdLen);    
    }
}

size_t
Metadata::sizeAsBytes() const 
{
    size_t len;
    len = 1 + taskId.getLength() + opId.getLength();
    if (options.getCount()) {
        len += 1 + options.getLength();
    }
    return len;
}

size_t
Metadata::sizeInArray(const u_int8_t *array, size_t len) 
{
    const u_int8_t *_p = array;
    size_t _remain = len;
    size_t _l;
    char _has_options;
    u_int8_t _flags;
    u_int8_t _task_id_len;
    u_int8_t _op_id_len;
    u_int8_t _opts_len;
    u_int8_t _version;
        
    if (!array)
        return 0;
    if (len < XTR_MD_MIN_LENGTH)
        return 0;
    //flags
    _flags = *_p;
    _version = (_flags & XTR_MASK_VERSION) >> 4;
    _op_id_len = (_flags & XTR_MASK_OPIDLEN) >> 3;

    //version must be 0 or 1 in this implementation
    //According to the spec, version 0 can't have the
    //opIdLen flag set to 1.
    //we will be liberal and try to read version 0 even
    //if it has the opIdLen flag set to 1. In this case
    //we will serialize the resulting metadata as version 1,
    //though.
    if (_version  > XTR_CURRENT_VERSION) {
        return 0;
    }
 
    _has_options = ((_flags & XTR_MASK_HASOPTION) != 0);
    _task_id_len = (_flags & XTR_MASK_IDLEN); // >> 0

    _p++; _remain--;
    //taskId
    switch(_task_id_len) {
    case 0:
        _l = 4;
        break;
    case 1:
        _l = 8;
        break;
    case 2:
        _l = 12;
        break;
    case 3:
        _l = 20;
        break;
    default: //can't happen
        _l = 0;
    }
    //taskId
    if (_remain < _l) {
        return 0;
    }
    _p += _l; _remain -= _l;
    //opId
    if (_op_id_len == 0) 
        _l = 4;
    else
        _l = 8;
 
    if (_remain < _l) 
        return 0;
    _p += _l; _remain -= _l;

    //options
    if (_has_options) {
        if (!_remain)
            return 0;
        _opts_len = *_p;
        _p++; _remain--;
        if (_opts_len == 0)
            return 0;
        if (_remain < _opts_len)
            return 0;
        _p += _opts_len;
        _remain -= _opts_len;
    }
    assert((unsigned)(_p - array) == (unsigned)(len - _remain));
    return (_p - array);
}

void
Metadata::clear() 
{
    //if (options.getCount())
        options.clear();
    taskId = TaskId();
    opId   = OpId();
}

bool 
Metadata::isEqual(const Metadata& other) const
{
    return (taskId.isEqual(other.taskId) &&
            opId.isEqual(other.opId) &&
            options.isEqual(other.options));    
}

xtr_result
Metadata::setTaskId(const TaskId &id) 
{
    if (id.isValid()) { 
        taskId = id;
        return XTR_SUCCESS;
    }    
    return XTR_FAIL;
}

xtr_result
Metadata::setRandomTaskId(size_t len)
{
    u_int8_t v[20];
    int i,lim;
    u_int32_t r;
    switch(len) {
    case 4:
    case 8:
    case 12:
    case 20:
        lim = len / 4;
        break;
    default: 
        return XTR_FAIL;
    }
    for (i = 0; i < lim; i++) {
        r = random();
        memcpy(&(v[i*4]), &r, 4);
    }
    return taskId.setBytes(v,len);
}

xtr_result
Metadata::setOpId(const OpId &id)
{
    opId = id;
    if (version == 0 && opId.getLength() == 8)
        version = 1;
    return XTR_SUCCESS;
}

xtr_result
Metadata::setRandomOpId(size_t len)
{
    u_int8_t v[8];
    int i,lim;
    u_int32_t r;
    switch(len) {
    case 4:
    case 8:
        lim = len / 4;
        break;
    default: 
        return XTR_FAIL;
    }
    for (i = 0; i < lim; i++) {
        r = random();
        memcpy(&(v[i*4]), &r, 4);
    }
    if (version == 0 && len == 8) 
        version = 1;
    return opId.setBytes(v,len);
}

xtr_result 
Metadata::pack(u_int8_t* dest, size_t *len) const
{
    u_int8_t *p = dest;
    size_t _remain = *len;
    size_t _len;
    size_t _l;
    u_int8_t _flags;
    if (!dest || !len)
        return XTR_FAIL;
    if (sizeAsBytes() > *len)
        return XTR_FAIL;
    assert(_remain >= 1);
    //construct _flags
    _flags = version << 4;
    assert(version == 0 || version == 1);
    _l = taskId.getLength();
    switch (_l) {
    case 4:
        break;
    case 8:
        _flags |= 1;
        break;
    case 12:
        _flags |= 2;
        break;
    case 20:
        _flags |= 3;
        break;
    default:
        assert(0);
    }
    if (opId.getLength() == 8)
        _flags |= XTR_MASK_OPIDLEN;   
    if (options.getCount())
        _flags |= XTR_MASK_HASOPTION;
    *p = _flags;
    p++; _remain--;
    //taskId
    assert(_remain >= _l);
    _len = _remain;
    taskId.pack(p, &_len);
    p += _len; _remain -= _len;
    //opID
    _len = _remain;
    opId.pack(p, &_len);
    p += _len; _remain -= _len;
    //options
    if (options.getCount()) {
        assert(options.getLength());
        assert(_remain >= 1);
        *p = options.getLength();
        p++; _remain--;
        assert(_remain >= options.getLength());
        _len = _remain;
        options.pack(p, &_len);
        p += _len; _remain -= _len;
        assert(_len == options.getLength());
    }
    assert((unsigned)(*len - _remain) == (unsigned)(p - dest));
    *len = p - dest;
    return XTR_SUCCESS;
}

char* 
Metadata::toString(char* dest, size_t len) const 
{
    size_t packed_length;
    u_int8_t buf[XTR_MD_MAX_LENGTH];
    char *p;
    unsigned int k;
    
    if (!dest)
        return dest;
    *dest = 0;   //empty string
    
    packed_length = sizeAsBytes();
    assert(packed_length <= XTR_MD_MAX_LENGTH);

    if (len < sizeAsString() + 1)
        return dest;

    if (pack(buf, &packed_length) != XTR_SUCCESS) {
        return dest;
    }
    
    /* now convert that binary representation into an hex string */
    k = 0;
    p = dest;
    while ( k < packed_length) {
        xtr_btoh(buf[k++],p);
        p += 2;
    }
    *p = 0;
    return dest;
}    

u_int16_t 
Metadata::getChainId() const
{
    const OptionChainId* c_id = 0;
    unsigned int i;
    u_int16_t r = 0;

    for (i = 0; i < options.getCount() && !c_id; i++)
        c_id = dynamic_cast<const OptionChainId*>(&options[i]);
    if (c_id)
        r = c_id->getId();
    return r;
}

xtr_result
Metadata::setChainId(u_int16_t id)
{
    //look for a chainId option. If not present, create one.
    OptionChainId* c_idp = 0;
    unsigned int i;
    xtr_result r = XTR_SUCCESS;

    for (i = 0; i < options.getCount() && !c_idp; i++)
        c_idp = dynamic_cast<OptionChainId*>(&options[i]);
    if (c_idp)
        c_idp->setId(id);
    else {
        OptionChainId c_id = OptionChainId(id);
        r = addOption(c_id);
    }
    return r;
}

/** Sets the chainId to a random value */
xtr_result
Metadata::newChainId()
{
    u_int16_t r;
    r = (u_int16_t)(random() >> 4);
    return setChainId(r);
}

/***** Convenience functions for the Severity option ****/
u_int8_t 
Metadata::getSeverity() const
{
    const OptionSeverity* opt_s = 0;
    unsigned int i;
    u_int8_t s = OptionSeverity::DEFAULT;

    for (i = 0; i < options.getCount() && !opt_s; i++)
        //dynamic_cast returns null if the cast fails
        opt_s = dynamic_cast<const OptionSeverity*>(&options[i]);
    if (opt_s)
        s = opt_s->getSeverity();
    return s;
}

xtr_result
Metadata::setSeverity(u_int8_t s)
{
    //look for a Severity option. If not present, create one.
    OptionSeverity* opt_sp = 0;
    unsigned int i;
    xtr_result r = XTR_SUCCESS;

    for (i = 0; i < options.getCount() && !opt_sp; i++)
        opt_sp = dynamic_cast<OptionSeverity*>(&options[i]);
    if (opt_sp)
        opt_sp->setSeverity(s);
    else {
        OptionSeverity opt_s = OptionSeverity(s);
        r = addOption(opt_s);
    }
    return r;
}

}; //namespace xtr
