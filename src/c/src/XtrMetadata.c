/*
* Copyright (c) 2005,2006,2007,2008 The Regents of the University of California.
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

#include "XtrMetadata-c.h"
#include <stdlib.h>
#include <stdint.h>

/********************** Data Structure Definitions *******************/
/* Task ID (part of the metadata) */
struct xtr_task_st {
   int len;
   union {
      uint8_t len4[4];
      uint8_t len8[8];
      uint8_t len12[12];
      uint8_t len20[20];
   } task;
};

/* Operation ID (part of the metadata) */
struct xtr_opid_st {
   int len;
   union {
      uint8_t len4[4];
      uint8_t len8[8];
   } opid;
};

/* Individual Option (part of the metadata) */
struct xtr_option_st {
   uint8_t type;
   uint8_t len;
   uint8_t payload[MAX_OPTION_LENGTH];
};

/* Options Block (part of the metadata)
   May contains multiple individual options */
struct xtr_options_st {
   int num_options;
   xtr_option_t * options;
};

/* Metadata Data Structure */
struct xtr_md_st {
   int version;
   xtr_task_t taskid;
   xtr_opid_t opid;
   xtr_options_t options;
};


/********************** Allocation Routines *******************/
